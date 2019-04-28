import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class VisionServer {
    static {
        // -Djava.library.path=$PROJECT_DIR$\opencv\x64
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * 捕获图像
     */
    private String image;

    /**
     * 显示图片列表
     */
    private ConcurrentHashMap<String, Mat> mats = new ConcurrentHashMap<>();

    /**
     * WebSocket客户端
     */
    private WebSocketClient client;

    /**
     * 坐标系映射矩阵
     */
    private Mat transformMatrix = new Mat(3, 3, CvType.CV_64FC1);

    public VisionServer(int port) throws URISyntaxException {
        client = new WebSocketClient(new URI("ws://localhost:" + port)) {
            @Override
            public void onMessage(String message) {
                if (message.startsWith("captureResult:")) {
                    image = message.substring("captureResult:".length());
                }
            }

            @Override
            public void onOpen(ServerHandshake handshake) {
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };

        transformMatrix.put(0, 0, -0.000101825605002298, 2.939243692861688e-08, 0.06646769634645851, -8.607060170109831e-08, -0.0001016594607596162, 0.04661288966452128, -3.343806158084244e-09, 1.60325558349952e-10, 0.0003408428257277698);
    }

    /**
     * 连接服务器
     */
    public void connect() {
        client.connect();
        while (!client.isOpen()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送数据
     *
     * @param content 数据
     */
    public void send(String content) {
        client.send(content);
    }

    /**
     * 获取图像
     *
     * @param timeout 超时时间
     * @return 图像
     */
    public Mat capture(int timeout) {
        client.send("capture");

        image = null;
        int elapsed = 0;
        while (elapsed <= timeout) {
            if (image != null) {
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            elapsed += 500;
        }

        if (image == null) {
            return null;
        }

        byte[] data = Base64.getDecoder().decode(image);
        Mat imageData = new Mat(data.length, 1, CvType.CV_8UC1);
        imageData.put(0, 0, data);

        Mat image = Imgcodecs.imdecode(imageData, Imgcodecs.IMREAD_COLOR);
        showImage("capture", image);

        return image;
    }

    /**
     * 显示图片
     *
     * @param name 名称
     * @param mat  图片
     */
    public void showImage(String name, Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        String data = new String(Base64.getEncoder().encode(buffer.toArray()), StandardCharsets.ISO_8859_1);
        client.send("saveImage:" + name + "," + URLEncoder.encode(data, Charset.forName("ascii")));
    }

    /**
     * 清空图片列表
     */
    public void clearImages() {
        client.send("clearImages");
    }

    /**
     * 标定
     */
    public void calibrate() {
        Mat image = capture(5 * 1000);
        if (image == null) {
            return;
        }

        Size imageSize = new Size(image.width(), image.height());
        Size boardSize = new Size(11, 8);
        int squareSize = 15;

        // 构造角点世界坐标
        List<Point3> point3s = new ArrayList<>();
        for (int row = 0; row < boardSize.height; row++) {
            for (int col = 0; col < boardSize.width; col++) {
                point3s.add(new Point3(col * squareSize, row * squareSize, 0));
            }
        }
        MatOfPoint3f objectPoints = new MatOfPoint3f();
        objectPoints.fromList(point3s);

        List<Mat> allCornerPoints = new ArrayList<>();
        List<Mat> allObjectPoints = new ArrayList<>();

        // 查找角点图像坐标
        MatOfPoint2f cornerPoints = new MatOfPoint2f();
        boolean isFind = Calib3d.findChessboardCorners(image, boardSize, cornerPoints, 0);
        if (isFind) {
            Imgproc.cornerSubPix(image, cornerPoints, new Size(11, 11), new Size(-1, -1), new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.001));
            allCornerPoints.add(cornerPoints);
            allObjectPoints.add(objectPoints);
        } else {
            return;
        }

        // 标定
        Mat cameraMatrix = new Mat();
        Mat distCoeffs = new Mat(5, 1, CvType.CV_64F);
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        Calib3d.calibrateCamera(allObjectPoints, allCornerPoints, imageSize, cameraMatrix, distCoeffs, rvecs, tvecs);
        transformMatrix = calcTransformMatrix(cameraMatrix, rvecs.get(0), tvecs.get(0));
    }

    /**
     * 二位坐标映射三维坐标
     *
     * @param point 二维坐标
     * @return 三维坐标
     */
    public Point3 transform(Point point) {
        double a1, a2, a3, a4, a5, a6, a7, a8, a9;

        a1 = transformMatrix.get(0, 0)[0];
        a2 = transformMatrix.get(0, 1)[0];
        a3 = transformMatrix.get(0, 2)[0];
        a4 = transformMatrix.get(1, 0)[0];
        a5 = transformMatrix.get(1, 1)[0];
        a6 = transformMatrix.get(1, 2)[0];
        a7 = transformMatrix.get(2, 0)[0];
        a8 = transformMatrix.get(2, 1)[0];
        a9 = transformMatrix.get(2, 2)[0];

        // 世界坐标中X值
        double x = (a1 * point.x + a2 * point.y + a3) / (a7 * point.x + a8 * point.y + a9);

        // 世界坐标中Y值
        double y = (a4 * point.x + a5 * point.y + a6) / (a7 * point.x + a8 * point.y + a9);

        return new Point3(x, y, 0);
    }

    /**
     * 计算坐标系映射矩阵
     *
     * @param cameraMatrix 内参矩阵
     * @param rvec         旋转矩阵
     * @param tvec         平移矩阵
     * @return 坐标系映射矩阵
     */
    public Mat calcTransformMatrix(Mat cameraMatrix, Mat rvec, Mat tvec) {
        Mat dst = new Mat(3, 3, CvType.CV_64F);
        Calib3d.Rodrigues(rvec, dst);

        dst.put(0, 2, tvec.get(0, 0));
        dst.put(1, 2, tvec.get(1, 0));
        dst.put(2, 2, tvec.get(2, 0));

        Mat h = new Mat();
        Core.gemm(cameraMatrix, dst, 1.0, Mat.zeros(cameraMatrix.size(), cameraMatrix.type()), 0.0, h);

        return h.inv();
    }

    private String getWebSocketId(WebSocket webSocket) {
        return webSocket.getRemoteSocketAddress().getAddress().getHostAddress() + ":" + webSocket.getRemoteSocketAddress().getPort();
    }
}
