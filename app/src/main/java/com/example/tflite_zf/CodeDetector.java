package com.example.tflite_zf;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class CodeDetector {
    private static final String TAG = "CodeDetector";

    private static final String MODEL_FILE_NAME = "model.tflite";

//  字符列表
    private static final String letters = "012345678abcdefghijklmnpqrstuvwxy";

    // Specify the output size
    private static final int NUMBER_LENGTH = letters.length();

    // Specify the input size
//    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 1;

    // Number of bytes to hold a float (32 bits / float) / (8 bits / byte) = 4 bytes / float
    private static final int BYTE_SIZE_OF_FLOAT = 4;

    int width = 12;
    int height = 23;

    int[][] pixels = new int[4][];

    // The tensorflow lite file
    private Interpreter tflite;

    // Input byte buffer
    private ByteBuffer inputBuffer;

    // Output array [1, 33]
    private float[][] output;

    public CodeDetector(Activity activity) {
        try {
            tflite = new Interpreter(loadModelFile(activity));
            inputBuffer = ByteBuffer.allocateDirect(
                    BYTE_SIZE_OF_FLOAT * width * height * DIM_PIXEL_SIZE);
            inputBuffer.order(ByteOrder.nativeOrder());
            output = new float[1][NUMBER_LENGTH];
            Log.d(TAG, "Created a Tensorflow Lite Classifier.");
        } catch (IOException e) {
            Log.e(TAG, "IOException loading the tflite file failed.");
        }
    }

    public void sliceImage(Bitmap bitmap) {
        // 图像原尺寸:72 * 27, 分割图像，各数字的开始位置
        int[] x = {5, 17, 29, 41};
        for (int i=0; i<x.length; i++) {
            pixels[i] = getBitmapPixels(bitmap, x[i], 0, width, height);
        }
    }

    public String detect() {
        String code = "";
        for (int i=0; i<4; i++) {
            prepareData(i);
            tflite.run(inputBuffer, output);
//            tflite.runForMultipleInputsOutputs();
            code += postProcess();
        }
        return code;
    }

    /**
     * Load the model file from the assets folder
     */
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE_NAME);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Converts it into the Byte Buffer to feed into the model
     */
    private void prepareData(int num) {
        // Reset the image data
        inputBuffer.rewind();
//        for (int[] pixel : pixels) {
            for (int color : pixels[num]) {
                int r = (color >> 16) & 0xff;
                int g = (color >>  8) & 0xff;
                int b = (color      ) & 0xff;
                // 彩色图转灰度图 依据ITU-R 601-2 luma transform
                //  L = R * 299/1000 + G * 587/1000 + B * 114/1000
                float l = r*0.299F + g*0.587F + b*0.114F;
                inputBuffer.putFloat(l >= 127.5 ? 1F : 0F);
            }
//        }
    }

    /**
     * Go through the output and find the number that was identified.
     *
     * @return the number that was identified (returns -1 if one wasn't found)
     */
    private String postProcess() {
        String code = "";
        for (float[] out : output) {
            code += postProcess(out);
        }
        return code;
    }

    private String postProcess(float[] out) {
        int maxIdx = 0;
        float maxValue = out[0];
        for (int i=1; i<out.length; i++) {
            if (out[i] > maxValue) {
                maxIdx = i;
                maxValue = out[i];
            }
        }
        return String.valueOf(letters.charAt(maxIdx));
    }

    static int[] getBitmapPixels(Bitmap bitmap, int x, int y, int width, int height) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), x, y,
                width, height);
        final int[] subsetPixels = new int[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(pixels, (row * bitmap.getWidth()),
                    subsetPixels, row * width, width);
        }
        return subsetPixels;
    }
}
