package com.udayraj.androidomr.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import com.udayraj.androidomr.constants.SC;
import com.udayraj.androidomr.view.Quadrilateral;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class provides utilities for camera.
 */

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static boolean compareFloats(double left, double right) {
        double epsilon = 0.00000001;
        return Math.abs(left - right) < epsilon;
    }

    public static Camera.Size determinePictureSize(Camera camera, Camera.Size previewSize) {
        if (camera == null) return null;
        Camera.Parameters cameraParams = camera.getParameters();
        List<Camera.Size> pictureSizeList = cameraParams.getSupportedPictureSizes();
        Collections.sort(pictureSizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size size1, Camera.Size size2) {
                Double h1 = Math.sqrt(size1.width * size1.width + size1.height * size1.height);
                Double h2 = Math.sqrt(size2.width * size2.width + size2.height * size2.height);
                return h2.compareTo(h1);
            }
        });
        Camera.Size retSize = null;

        // if the preview size is not supported as a picture size
        float reqRatio = ((float) previewSize.width) / previewSize.height;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        for (Camera.Size size : pictureSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
            if (Utils.compareFloats(deltaRatio, 0)) {
                break;
            }
        }

        return retSize;
    }

    public static Camera.Size getOptimalPreviewSize(Camera camera, int w, int h) {
        if (camera == null) return null;
        final double targetRatio = (double) h / w;
        Camera.Parameters cameraParams = camera.getParameters();
        List<Camera.Size> previewSizeList = cameraParams.getSupportedPreviewSizes();
        Collections.sort(previewSizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size size1, Camera.Size size2) {
                double ratio1 = (double) size1.width / size1.height;
                double ratio2 = (double) size2.width / size2.height;
                Double ratioDiff1 = Math.abs(ratio1 - targetRatio);
                Double ratioDiff2 = Math.abs(ratio2 - targetRatio);
                if (Utils.compareFloats(ratioDiff1, ratioDiff2)) {
                    Double h1 = Math.sqrt(size1.width * size1.width + size1.height * size1.height);
                    Double h2 = Math.sqrt(size2.width * size2.width + size2.height * size2.height);
                    return h2.compareTo(h1);
                }
                return ratioDiff1.compareTo(ratioDiff2);
            }
        });

        return previewSizeList.get(0);
    }

    public static int configureCameraAngle(Activity activity) {
        int angle;

        Display display = activity.getWindowManager().getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0: // This is display orientation
                angle = 90; // This is camera orientation
                break;
            case Surface.ROTATION_90:
                angle = 0;
                break;
            case Surface.ROTATION_180:
                angle = 270;
                break;
            case Surface.ROTATION_270:
                angle = 180;
                break;
            default:
                angle = 90;
                break;
        }

        return angle;
    }

    public static Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = {null, null, null, null};

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.compare(lhs.y + lhs.x,rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.compare(lhs.y - lhs.x, rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);
        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator);
        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);
        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    //    needed coz of the mess opencv-java has made:
    private static MatOfPoint hull2Points(MatOfInt hull, MatOfPoint contour) {
        List<Integer> indexes = hull.toList();
        List<Point> points = new ArrayList<>();
        MatOfPoint point= new MatOfPoint();
        for(Integer index:indexes) {
            points.add(contour.toList().get(index));
        }
        point.fromList(points);
        return point;
    }
    private static List<MatOfPoint> getTopContours(Mat inputMat) {
        int MAX_TOP_CONTOURS = 10;
        Mat mHierarchy = new Mat();
        List<MatOfPoint> mContourList = new ArrayList<>();
        //finding contours - RETR_LIST is (faster, thus) better as we are sorting by area anyway
        Imgproc.findContours(inputMat, mContourList, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        // convert contours to its convex hulls
        List<MatOfPoint> mHullList = new ArrayList<>();
        MatOfInt tempHullIndices = new MatOfInt();
        for (int i = 0; i < mContourList.size(); i++) {
            Imgproc.convexHull(mContourList.get(i), tempHullIndices);
            mHullList.add(hull2Points(tempHullIndices, mContourList.get(i)));
        }

        if (mHullList.size() != 0) {
            Collections.sort(mHullList, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                    return Double.compare(Imgproc.contourArea(rhs),Imgproc.contourArea(lhs));
                }
            });
            mHullList = mHullList.subList(0, Math.min(mHullList.size(), MAX_TOP_CONTOURS));
            return mHullList;
        }
        return null;
    }

    private static int distance(Point a,Point b) {
        double xDiff = a.x - b.x;
        double yDiff = a.y - b.y;
        return (int) Math.sqrt(Math.pow(xDiff,2) + Math.pow(yDiff, 2));
    }
    private static Quadrilateral findQuadrilateral(List<MatOfPoint> mContourList) {
        for (MatOfPoint c : mContourList) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.025 * peri, true);
            Point[] points = approx.toArray();
            // select biggest 4 angles polygon
            if (approx.rows() == 4) {
                Point[] foundPoints = sortPoints(points);
                return new Quadrilateral(approx, foundPoints);
            }
        }
        return null;
    }

    public static void drawContours(Mat processedMat) {
        List<MatOfPoint> contours = getTopContours(processedMat);
        if(contours == null) {
            Log.d(TAG, "No Contours found! ");
            return;
        }
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(processedMat, contours, i, new Scalar(155, 155, 155), 3);
        }
    }

    public static Mat preProcessMat(Mat mat){
        Mat processedMat = Utils.resize_util(mat, SC.uniform_width_hd, SC.uniform_height_hd);
        Imgproc.cvtColor(processedMat, processedMat, Imgproc.COLOR_BGR2GRAY, 4);
        normalize(processedMat);
        // Imgproc.blur(processedMat, processedMat, new Size(SC.KSIZE_BLUR, SC.KSIZE_BLUR));
        return processedMat;
    }

    public static void normalize(Mat processedMat){
        Core.normalize(processedMat, processedMat, 0, 255, Core.NORM_MINMAX);
    }
    public static void thresh(Mat processedMat) {
        Imgproc.threshold(processedMat, processedMat, 150, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
    }
    public static void canny(Mat processedMat) {
        Imgproc.Canny(processedMat, processedMat, SC.CANNY_THRESHOLD_U, SC.CANNY_THRESHOLD_L);
    }
    public static void morph(Mat processedMat) {
        // Close the small holes, i.e. Complete the edges on canny image
        Mat kernel = new Mat(new Size(SC.KSIZE_CLOSE, SC.KSIZE_CLOSE), CvType.CV_8UC1, new Scalar(255));
        Imgproc.morphologyEx(processedMat, processedMat, Imgproc.MORPH_CLOSE, kernel, new Point(-1,-1),1);
    }

    public static Quadrilateral findPage(Mat inputMat) {
        Mat processedMat = new Mat();
        inputMat.copyTo(processedMat);
        //Better results than threshold : Canny then Morph
        canny(processedMat);
        morph(processedMat);

        List<MatOfPoint> sortedContours = getTopContours(processedMat);
        processedMat.release();
        if (null != sortedContours) {
            Quadrilateral mLargestRect = findQuadrilateral(sortedContours);
            if (mLargestRect != null)
                return mLargestRect;
        }
        return null;
    }

    public static void logShape(String name, Mat m) {
        Log.d("custom"+TAG, "matrix: "+name+" shape: "+m.rows()+"x"+m.cols());
    }
    public static Point[] checkForMarkers(Mat warpLevel1, Point[] points, Mat marker) {
//        TODO FULL quadrant template matching here
//        TODO Make this run less frequently
        // matchOut will be a float image now!
        Mat matchOut = new Mat(new Size(warpLevel1.cols() - marker.cols()+1,warpLevel1.rows() - marker.rows()+1 ), CvType.CV_32FC1);
        //Template matching method : TM_CCOEFF_NORMED works best
        Imgproc.matchTemplate(warpLevel1, marker, matchOut, Imgproc.TM_CCOEFF_NORMED);

        Core.MinMaxLocResult mmr = Core.minMaxLoc(matchOut);
        Point matchLoc = mmr.maxLoc;
        // Log.d("customPointLoc",""+matchLoc);

        return new Point[] {matchLoc};
    }

    public static Mat four_point_transform(Mat inputMat, Point[] points) {
        // points are wrt Mat indices _// (as Returned by approxPolyDP for eg) (x+Mat.cols() used in template matching)
        //obtain a consistent order of the points : (tl, tr, br, bl)
        points = sortPoints(points);
        // compute the width of the new image,
        int resultWidth = (int) Math.max(distance(points[3],points[2]), distance(points[1],points[0]));
        // compute the height of the new image,
        int resultHeight = (int) Math.max(distance(points[2],points[1]), distance(points[3],points[0]));
        /*
         * now that we have the dimensions of the new image, construct
         * the set of destination points to obtain a "birds eye view",
         * (i.e. top-down view) of the image, again specifying points
         * in the top-left, top-right, bottom-right, and bottom-left
         * order
         */
        Point[] dst = new Point[] {
                new Point(0, 0),
                new Point(resultWidth - 1, 0),
                new Point(resultWidth - 1 , resultHeight - 1),
                new Point(0, resultHeight - 1)
        };

        // Some Java excess code -
        List<Point> pointsList = Arrays.asList(points);
        List<Point> dest = Arrays.asList(dst);
        Mat startM = Converters.vector_Point2f_to_Mat(pointsList);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);
        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC1);

        //compute the perspective transform matrix and then apply it
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);
        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight));

        return  outputMat;
    }

    public static Bitmap cropBitmap(Bitmap image, Point[] points) {
        points = sortPoints(points);
        // compute the width of the new image,
        int resultWidth = (int) Math.max(distance(points[3],points[2]), distance(points[1],points[0]));
        // compute the height of the new image,
        int resultHeight = (int) Math.max(distance(points[2],points[1]), distance(points[3],points[0]));
        // Some Android-java excess code -
        Mat inputMat = new Mat(image.getHeight(), image.getHeight(), CvType.CV_8UC1);
        org.opencv.android.Utils.bitmapToMat(image, inputMat);

        Mat outputMat = four_point_transform(inputMat, points);

        Bitmap output = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(outputMat, output);

        return output;
    }

    public static Bitmap decodeBitmapFromFile(String path, String imageName) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        return BitmapFactory.decodeFile(new File(path, imageName).getAbsolutePath(),
                options);
    }


    public static double getMaxCosine(double maxCosine, Point[] approxPoints) {
        for (int i = 2; i < 5; i++) {
            double cosine = Math.abs(angle(approxPoints[i % 4], approxPoints[i - 2], approxPoints[i - 1]));
            maxCosine = Math.max(cosine, maxCosine);
        }
        return maxCosine;
    }

    private static double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    public static Bitmap decodeBitmapFromByteArray(byte[] data, int reqWidth, int reqHeight) {
        // Raw height and width of image
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        options.inSampleSize = inSampleSize;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    @Deprecated
    public static Bitmap loadEfficientBitmap(byte[] data, int width, int height) {
        Bitmap bmp;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return bmp;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap resizeToScreenContentSize(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static Mat resize_util(Mat image, int u_width, int u_height) {
        Size sz = new Size(u_width,u_height);
        Mat resized = new Mat();
        if(image.cols() > u_width)
            // for downscaling
            Imgproc.resize(image,resized ,sz, 0,0 ,Imgproc.INTER_AREA);
        else
            // for upscaling
            Imgproc.resize(image,resized ,sz, 0,0 ,Imgproc.INTER_CUBIC);
        return resized;
    }
    public static Mat resize_util(Mat image, int u_width) {
        int u_height = (image.rows() * u_width)/image.cols();
        return resize_util(image,u_width,u_height);
    }

    public static Bitmap matToBitmapRotate(Mat processedMat){
        Bitmap cameraBitmap = Bitmap.createBitmap(processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(processedMat, cameraBitmap);
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.postRotate(90);
        // filter = true does the applyTransform here!
        return Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), rotateMatrix, true);
    }


    //UNUSED -
    public static Bitmap rotateBitmap(Bitmap cameraBitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        // filter = true does the applyTransform here!
        return Bitmap.createBitmap(cameraBitmap, 0, 0, cameraBitmap.getWidth(), cameraBitmap.getHeight(), matrix, true);
    }
    public static Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }

            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }


    public static Point[] getPolygonDefaultPoints(int width, int height) {
        return new Point [] {
                new Point((int) (width * 0.14f), (int) (height * 0.13f)),
                new Point((int) (width * 0.84f), (int) (height * 0.13f)),
                new Point((int) (width * 0.14f), (int) (height * 0.83f)),
                new Point((int) (width * 0.84f), (int) (height * 0.83f))
        };
    }
}
