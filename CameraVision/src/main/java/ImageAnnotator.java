import java.util.Arrays;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Take a raw image from the camera and use the image
 * pipeline interpreter to mark up the processed image
 * for viewing by driver station.
 */
public class ImageAnnotator {
  private final HatchTargetPipelineInterpreter interpreter;
  private Mat outputImage;
  private final Scalar targetingRectangleColor;
  private final Scalar hatchTargetRectangleColor;

  /**
   * Construct an annotator with an instantiated interpreter.
   * 
   * @param interpreter An interpreter with a processed pipeline.
   */
  public ImageAnnotator(HatchTargetPipelineInterpreter interpreter) {
    if (interpreter == null) {
      throw new IllegalArgumentException("Interpreter cannot be null");
    }
    this.interpreter = interpreter;
    this.outputImage = new Mat();
    this.targetingRectangleColor = new Scalar(81, 190, 0);      // green
    this.hatchTargetRectangleColor = new Scalar(255, 51, 0);    // blue
  }

  public Mat annotate(Mat inputImage) {
    inputImage.copyTo(outputImage);
    drawTargetingRectangles();
    drawHatchTargetRectangles();
    printDistanceToHatchTargetInInches();
//    printWidthOfRectangles();
    return outputImage;
  }

  /**
   * Draw rectangles for all targeting tape found.
   */
  private void drawTargetingRectangles() {
    // Draw best-fit rectangles around targets
    for (RotatedRect rotatedRect: interpreter.getRectangles()) {
      drawRotatedRect(rotatedRect, targetingRectangleColor, 4);
    }        
  }

  /**
   * Draw rectangles for identified hatch targets.
   */
  private void drawHatchTargetRectangles() {
    for (HatchTarget hatchTarget: interpreter.getHatchTargets()) {
      drawRotatedRect(hatchTarget.targetRectangle(), hatchTargetRectangleColor, 4);
    }
  }

  private void printWidthOfRectangles() {
    for (RotatedRect rotatedRect: interpreter.getRectangles()) {
      Point[] vertices = new Point[4];
      rotatedRect.points(vertices);
      Point textStart = vertices[0];
      textStart.y += 10;
      long width = Math.round((rotatedRect.size.width < rotatedRect.size.height ? rotatedRect.size.width : rotatedRect.size.height) * 100);
      double roundedWidth = ((double)width)/100;
      Imgproc.putText(outputImage, Double.toString(roundedWidth), textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, new Scalar(2,254,255));
    }
  }

  private void printDistanceToHatchTargetInInches() {
    for (HatchTarget hatchTarget: interpreter.getHatchTargets()) {
      Point[] vertices = new Point[4];
      RotatedRect rotatedRect = hatchTarget.targetRectangle();
      rotatedRect.points(vertices);
      Point textStart = vertices[0];
      textStart.y += 10;
      long distance = Math.round(hatchTarget.rangeInInches() * 10);
      double roundedDistance = ((double)distance)/10;
      Imgproc.putText(outputImage, "d: " + Double.toString(roundedDistance), textStart, Core.FONT_HERSHEY_COMPLEX_SMALL, .75, new Scalar(2,254,255));
    }
  }

  /**
   * Helper routine to draw rotated rectangles.
   * 
   * @param rotatedRect The rectangle to draw.
   * @param color       The color of the rectangle.
   * @param thickness   The thickness of the rectangle.
   */
  private void drawRotatedRect(RotatedRect rotatedRect, Scalar color, int thickness) {
    Point[] vertices = new Point[4];
    rotatedRect.points(vertices);
    MatOfPoint points = new MatOfPoint(vertices);
    Imgproc.drawContours(outputImage, Arrays.asList(points), -1, color, thickness);
  }
}