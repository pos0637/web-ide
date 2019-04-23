import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void test(VisionServer s, Scalar lower, Scalar upper, double minArea, double maxArea) {
        byte[] data = s.capture(1);
        if (data == null) {
            return;
        }

        Mat imageData = new Mat(data.length, 1, CvType.CV_8UC1);
        imageData.put(0, 0, data);
        Mat image = Imgcodecs.imdecode(imageData, Imgcodecs.IMREAD_COLOR);
        s.showImage("image", image);

        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV_FULL);

        Mat mask = new Mat();
        Core.inRange(hsv, lower, upper, mask);
        s.showImage("mask", mask);

        Mat gray = new Mat();
        Imgproc.threshold(mask, gray, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
        s.showImage("gray", gray);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

        for (MatOfPoint contour : contours) {
            MatOfPoint2f c = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approx = new MatOfPoint2f();
            double epsilon = 0.01 * Imgproc.arcLength(c, true);
            Imgproc.approxPolyDP(c, approx, epsilon, true);
            double area = Imgproc.contourArea(approx);
            if ((minArea <= area) && (maxArea >= area)) {
                RotatedRect rect = Imgproc.minAreaRect(approx);
                System.out.println(String.format("center: (%f, %f), angle: %f", rect.center.x, rect.center.y, rect.angle));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Hello World!");

        int port = 8887;
        VisionServer s = new VisionServer(port);
        s.connect();

        test(s, new Scalar(100, 43, 46), new Scalar(124, 255, 255), 10000, 120000);

        while (true) {
            Thread.sleep(1000);
        }
    }
}
