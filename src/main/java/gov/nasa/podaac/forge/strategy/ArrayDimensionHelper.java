package gov.nasa.podaac.forge.strategy;

import ucar.ma2.Array;

public class ArrayDimensionHelper {
    public static class ArrayData {
        public final float[][] data2D;
        public final int size_x;
        public final int size_y;
        public final int size_z;
        public final boolean is3D;

        private ArrayData(float[][] data2D, int size_x, int size_y, int size_z, boolean is3D) {
            this.data2D = data2D;
            this.size_x = size_x;
            this.size_y = size_y;
            this.size_z = size_z;
            this.is3D = is3D;
        }
    }

    public static ArrayData convertToWorkingArray(Array inputArray, double fillValue) {
        Object javaArray = inputArray.copyToNDJavaArray();
        
        if (javaArray instanceof float[][][]) {
            // Handle 3D array
            float[][][] array3D = (float[][][]) javaArray;
            int size_x = array3D.length;
            int size_y = array3D[0].length;
            int size_z = array3D[0][0].length;
            
            float[][] result = new float[size_x][size_y];
            // Initialize with fill value
            for (int x = 0; x < size_x; x++) {
                for (int y = 0; y < size_y; y++) {
                    result[x][y] = (float) fillValue;
                }
            }
            
            return new ArrayData(result, size_x, size_y, size_z, true);
            
        } else if (javaArray instanceof float[][]) {
            // Handle 2D array
            float[][] array2D = (float[][]) javaArray;
            int size_x = array2D.length;
            int size_y = array2D[0].length;
            
            return new ArrayData(array2D, size_x, size_y, 1, false);
            
        } else {
            throw new IllegalArgumentException("Unsupported array type: " + javaArray.getClass());
        }
    }
}
