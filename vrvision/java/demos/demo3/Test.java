import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class Test {
    /**
     * 获取图像
     */
    public static void testHsv(VisionServer server) {
        Mat image = server.capture(3 * 1000);
        if (image == null) {
            return;
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV_FULL);

        List<Mat> channels = new ArrayList<>();
        Core.split(hsv, channels);

        Mat h = channels.get(0);
        Mat s = channels.get(1);
        Mat v = channels.get(2);

        server.showImage("h", h);
        server.showImage("s", s);
        server.showImage("v", v);
    }

    /**
     * 二值化单元
     */
    public static void testThreshold(VisionServer server) {
        Mat image = server.capture(3 * 1000);
        if (image == null) {
            return;
        }

        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.threshold(gray, gray, 100, 200, Imgproc.THRESH_BINARY);
        server.showImage("gray", gray);
    }

    /**
     * 定位单元
     */
    public static void testPositioning(VisionServer server) {
        final int minArea = 2000;
        final int maxArea = 100000;

        Mat image = server.capture(3 * 1000);
        if (image == null) {
            return;
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV_FULL);

        Mat mask = new Mat();
        Core.inRange(hsv, new Scalar(40, 170, 200), new Scalar(50, 186, 220), mask);
        // Core.inRange(hsv, new Scalar(160, 200, 140), new Scalar(180, 230, 250), mask);
        server.showImage("hsv", hsv);
        server.showImage("mask", mask);

        Mat gray = new Mat();
        Imgproc.threshold(mask, gray, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
        server.showImage("gray", gray);

        for (MatOfPoint contour : contours) {
            MatOfPoint2f c = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approx = new MatOfPoint2f();
            double epsilon = 0.01 * Imgproc.arcLength(c, true);
            Imgproc.approxPolyDP(c, approx, epsilon, true);
            RotatedRect rect = Imgproc.minAreaRect(approx);
            double area = Imgproc.contourArea(approx);
            if ((minArea <= area) && (maxArea >= area)) {
                Moments moments = Imgproc.moments(contour);
                if (moments.m00 == 0) {
                    continue;
                }

                Point point = new Point(moments.m10 / moments.m00, moments.m01 / moments.m00);
                Point3 point3 = server.transform(point);
                server.send("move:" + point3.x + "," + point3.y + "," + rect.angle);
            }
        }
    }

    /**
     * 标定单元
     */
    public static void testCalibration(VisionServer server) {
        server.calibrate();
    }

    /**
     * 映射单元
     */
    public static void testTransform(VisionServer server) {
        Point point = new Point(100, 100);
        Point3 point3 = server.transform(point);
        server.send("move:" + point3.x + "," + point3.y + "," + 0);
    }

    public static void main(String[] args) throws Exception {
        VisionServer server = new VisionServer(8887);
        server.connect();

        while (true) {
            server.clearImages();
            testThreshold(server);
            Thread.sleep(3 * 1000);
        }
    }
}
