import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Test {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello World!");

        int port = 8887;
        VisionServer s = new VisionServer(port);
        s.start();

        Mat m = Mat.eye(3, 3, CvType.CV_8UC1);
        System.out.println(m.dump());

        while (true) {
            Thread.sleep(1000);
        }
    }
}
