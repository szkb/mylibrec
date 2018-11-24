package net.librec.math.structure;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * @author szkb
 * @date 2018/11/13 20:50
 */
public class SparseMatrixTest {

    public static void main(String[] args) {
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        dataTable.put(1, 2, 1.0);
        colMap.put(2, 1);
        dataTable.put(1, 0, 1.0);
        colMap.put(0, 1);
        dataTable.put(2, 0, 1.0);
        colMap.put(0, 2);
        dataTable.put(1, 1, 0.5);
        colMap.put(1, 1);
        dataTable.put(1, 3, 0.1);
        colMap.put(3, 1);


        SparseMatrix matrix = new SparseMatrix(5, 5, dataTable, colMap);

//        System.out.println(matrix);
//        System.out.println(dataTable);
//        System.out.println(colMap);
//        System.out.println(matrix.rows());
//        System.out.println(matrix.row(2));
//        System.out.println(matrix.row(3));

//        System.out.println(matrix.row(1));
//        System.out.println(matrix.row(1).get(0));
//        System.out.println(matrix.row(1).get(1));
        // 得到信任的用户
//        System.out.println(matrix.getColumns(1));
//        System.out.println(matrix.getColumns(2));
//        System.out.println(matrix.getColumns(0));

//        System.out.println(matrix.get(1, 2));
//        if (Math.abs(0.8 - 1) <= 0.000001) {
//            System.out.println("Yes");
//        }
//        System.out.println(matrix.contains(1, 0));
//        System.out.println(matrix.contains(1, 1));
//        System.out.println(matrix.get(1, 1));
//        System.out.println(matrix.numColumns);
//        System.out.println(matrix.numRows);
//        System.out.println(matrix.row(1).mean());
//        System.out.println(matrix.row(1).getCount());
//        SparseMatrix sparseMatrix = new SparseMatrix(matrix);
//        System.out.println(sparseMatrix);
//        System.out.println();
//        matrix.set(1, 1, 0.001);
//        System.out.println(sparseMatrix);
//        System.out.println();
//        System.out.println(matrix);
//
//        System.out.println(matrix.row(100).size());
//        System.out.println((matrix.row(100).mean() > 0 ? "yes" : "no"));
//
//
//        System.out.println(matrix.row(1).mean());
//
//        SparseVector sparseVector = matrix.row(1);
//        for (VectorEntry vectorEntry : sparseVector) {
//            System.out.println(vectorEntry.get());
//        }
//
//        System.out.println(Math.pow(3, 3));
//        matrix.add(2, 1, 1.0);
////        matrix.reshape(6, 6);
//        System.out.println(matrix);
//        dataTable.put(1, 2, 0.5);
//        colMap.put(2, 1);
//        matrix = new SparseMatrix(5, 5, dataTable, colMap);
//
//        System.out.println(matrix);
//        System.out.println(matrix.size());


//        System.out.println(matrix.get(1, 5) > 0 ? "yse" : "no");
        System.out.println(matrix);

//        System.out.println(matrix.contains(1, 3));
//        System.out.println(matrix.get(1, 3));
//        Set<Integer> userSet = matrix.getRowsSet(0);
//        System.out.println(userSet);
        System.out.println(matrix.columnSize(0));
        System.out.println(matrix.rowSize(1));
//        for (MatrixEntry socialMatrixEntry : matrix) {
//            System.out.print(socialMatrixEntry.row() + " ");
//            System.out.println(socialMatrixEntry.column());
//        }

    }
}
