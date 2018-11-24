/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.convertor.appender;

import com.google.common.collect.*;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.DataAppender;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.AbstractRecommender;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;

/**
 * A <tt>SocialDataAppender</tt> is a class to process and store social appender
 * data.
 *
 * @author SunYatong
 */
public class SocialDataAppender extends Configured implements DataAppender {

    /**
     * The size of the buffer
     */
    private static final int BSIZE = 1024 * 1024;

    /**
     * a {@code SparseMatrix} object build by the social data
     */
    private SparseMatrix userSocialMatrix;

    private SparseMatrix userSocialMatrix1;

    private SparseMatrix userSocialMatrix2;

    private SparseMatrix userSocialMatrix3;

    private SparseMatrix impUserSocialMatrix;

    /**
     * The path of the appender data file
     */
    private String inputDataPath;

    /**
     * User {raw id, inner id} map from rating data
     */
    private BiMap<String, Integer> userIds;

    /**
     * Item {raw id, inner id} map from rating data
     */
    private BiMap<String, Integer> itemIds;


    /*   对信任矩阵的自我研究空间  */


    /**
     * Initializes a newly created {@code SocialDataAppender} object with null.
     */
    public SocialDataAppender() {
        this(null);
    }

    /**
     * Initializes a newly created {@code SocialDataAppender} object with a
     * {@code Configuration} object
     *
     * @param conf {@code Configuration} object for construction
     */
    public SocialDataAppender(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Process appender data.
     *
     * @throws IOException if I/O error occurs during processing
     */
    @Override
    public void processData() throws IOException {
        if (conf != null && StringUtils.isNotBlank(conf.get("data.appender.path"))) {
            inputDataPath = conf.get("dfs.data.dir") + "/" + conf.get("data.appender.path");
            System.out.println(inputDataPath);
            readData(inputDataPath);

        }
    }

    /**
     * Read data from the data file. Note that we didn't take care of the
     * duplicated lines.
     *
     * @param inputDataPath the path of the data file
     * @throws IOException if I/O error occurs during reading
     */
    public void readData(String inputDataPath) throws IOException {

        int count1 = 0;//记录可信度的范围人数
        int count = 0;//记录专家的人数
        Double max = 0.0;
        int j = 0;
        double[] A = new double[1508];//保存专家的可信度
        double[] B = new double[1508];
        double[] global = new double[1508];
        int[] hash = new int[1508];//保存每个用户被信任的次数
        int[] E = new int[1508];//保存专家对应的索引和被信任的次数
        for (int i = 0; i < 1508; i++) {
            hash[i] = 0;
            B[i] = 0.0;
        }

        int[] C = new int[1600];
        int c = 0;
//        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        Set<Integer> set = new HashSet<Integer>();


        // Table {row-id, col-id, rate}
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Table<Integer, Integer, Double> dataTable1 = HashBasedTable.create();
        Table<Integer, Integer, Double> dataTable2 = HashBasedTable.create();
        // Map {col-id, multiple row-id}: used to fast build a rating matrix
        Multimap<Integer, Integer> colMap = HashMultimap.create();
        Multimap<Integer, Integer> colMap1 = HashMultimap.create();
        Multimap<Integer, Integer> colMap2 = HashMultimap.create();
        // BiMap {raw id, inner id} userIds, itemIds
        final List<File> files = new ArrayList<File>();
        final ArrayList<Long> fileSizeList = new ArrayList<Long>();
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                fileSizeList.add(file.toFile().length());
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        Files.walkFileTree(Paths.get(inputDataPath), finder);
        long allFileSize = 0;
        for (Long everyFileSize : fileSizeList) {
            allFileSize = allFileSize + everyFileSize.longValue();
        }
        // loop every dataFile collecting from walkFileTree
        for (File dataFile : files) {
//            System.out.print(dataFile);
            FileInputStream fis = new FileInputStream(dataFile);
            FileChannel fileRead = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(BSIZE);
            int len;
            String bufferLine = new String();
            byte[] bytes = new byte[BSIZE];
            while ((len = fileRead.read(buffer)) != -1) {
                buffer.flip();
                buffer.get(bytes, 0, len);
                bufferLine = bufferLine.concat(new String(bytes, 0, len)).replaceAll("\r", "\n");
                String[] bufferData = bufferLine.split("(\n)+");
                boolean isComplete = bufferLine.endsWith("\n");
                int loopLength = isComplete ? bufferData.length : bufferData.length - 1;
//                System.out.print(loopLength+"\n");
//                System.out.print(userIds+"\n");
                for (int i = 0; i < loopLength; i++) {
                    String line = new String(bufferData[i]);
                    String[] data = line.trim().split("[ \t,]+");
                    String userA = data[0];
//                    set.add(userA);
//                    System.out.print(userA+"\n");
                    String userB = data[1];
//                    set.add(userB);
//                    System.out.print(userB+"\n");
                    Double rate = (data.length >= 3) ? Double.valueOf(data[2]) : 1.0;
                    if (userIds.containsKey(userA) && userIds.containsKey(userB)) {

                        /* 将userID映射信任矩阵的索引 */
                        int row = userIds.get(userA);
                        set.add(row);
//                        hash[row]++;//这里有加东西哦！
//                        System.out.println("****************************");
//                        System.out.print(userA+"\n");
//                       System.out.print(row+"\n");
                        int col = userIds.get(userB);
                        set.add(col);
                        hash[col]++;//这里有加东西哦！
//                        System.out.print(userB+"\n");
//                       System.out.print(col+"\n");

                        dataTable.put(row, col, rate);
                        dataTable1.put(row, col, rate);
                        colMap.put(col, row);
                        colMap1.put(col, row);
                    }
                }
                if (!isComplete) {
                    bufferLine = bufferData[bufferData.length - 1];
                }
                buffer.clear();
            }
            fileRead.close();
            fis.close();
        }


        int numRows = userIds.size(), numCols = userIds.size();
        Arrays.sort(hash);
        int MMax = -1;
        int MMin = 2000;
        double MIMI = -1.0;
        double NINI = 2000.0;
//        Arrays.sort(hash);//这里一定要排序
        for (int i = hash.length - 1; i >= 0; i--) {
            if (hash[i] > 0) {
                if (hash[i] > MMax) MMax = hash[i];
                if (hash[i] < MMin) MMin = hash[i];
            }
        }
//        double dd = Math.log10(MMax) - Math.log10(MMin);
        double dd = MMax - MMin;
        int countHash = 0;
        for (int i = 0; i < 1508; i++) {
            if (hash[i] > 0) {
                countHash++;
//                global[i] = (Math.log10(hash[i]) - Math.log10(MMin)) / dd;
                global[i] = ((hash[i]) - (MMin)) / dd * Math.log10(1508 / 596);
//                System.out.println(global[i]);

            }
        }
        System.out.println("hash不为0的个数：" + countHash);
//System.out.println("总的个数："+set.size());

        for (int i = hash.length - 1; i >= 0; i--) {
            if (count > 75) break;
            if (hash[i] != 0) {
                count++;
//                System.out.println(hash[i]);
                E[j++] = i;
//                A[i]= hash[i]*1.00/45;
                A[i] = hash[i] * 1.00;
//                System.out.println("排名第"+i+"个数的全局信任值："+global[i]);
            }
        }
        userSocialMatrix1 = new SparseMatrix(numRows, numCols, dataTable, colMap);
//        System.out.println(userSocialMatrix1);
//        userSocialMatrix1.set(6,811,2.000);
//        System.out.println(userSocialMatrix1.get(6,1065));
//        System.out.println(userSocialMatrix1.contains(6,1065));
//        userSocialMatrix1.set(6,1065,10.00);
//        System.out.println(userSocialMatrix1.get(6,1065));
//        System.out.println(userSocialMatrix1);
        for (int i = 0; i < 1508; i++) {
            int count2 = 0;
            for (int k = 0; k < j; k++) {
                if (userSocialMatrix1.contains(E[k], i) && userSocialMatrix1.get(E[k], i) > 0) {
//                System.out.println(userSocialMatrix.get(E[k],i));
//                    System.out.println("行:"+E[k]+"列:"+i);
//                    B[i]+=A[E[k]]*userSocialMatrix1.get(E[k],i);//这里只能得到44个数字？？？
                    count2++;
                    B[i] += global[E[k]] * userSocialMatrix1.get(E[k], i);
//                    B[i]+=global[E[k]]*userSocialMatrix1.get(E[k],i)*a;这里的a可以认为是某个衰减系数
                }

            }
        }
//        for(int i = 0; i < j; i++){
//          for(int k = 0; k < 1508; k++){
//              if(!userSocialMatrix2.contains(k, E[i]) && set.contains(k) && set.contains(E[i])){
//                  dataTable1.put(k, E[i], 0.1);
//                  colMap1.put(E[i], k);
//              }
//          }
//        }
        for (int i3 = 0; i3 < 1508; i3++) {
            if (B[i3] > 0) {
//                System.out.println(B[i]);
                count1++;
//                for (int k = 0; k < 1508 && k != i; k++) {
//                    if (!userSocialMatrix1.contains(k, i) && set.contains(k) && set.contains(i)) {
////                        if(B[i] <= 0.3){
////                            dataTable1.put(k, i, 0.2);
////                            colMap1.put(i, k);
////                        }else {
////                            dataTable1.put(k, i, 0.3);
////                            colMap1.put(i, k);
////
////                        }
////                        dataTable1.put(k, i, 0.25);
////                        colMap1.put(i, k);
//
//
//                    }
//                }
            }
        }


        System.out.println("count1:" + count1);
        userSocialMatrix3 = new SparseMatrix(numRows, numCols, dataTable1, colMap1);
//        System.out.print(numRows+"\n");
//        System.out.println(numCols);
        // build rating matrix
//        userSocialMatrix1 = new SparseMatrix(numRows, numCols, dataTable, colMap);
//        System.out.println(userSocialMatrix);

        int h1 = 0;
        int h2 = 0;
        for (int i1 = 0; i1 < 1508; i1++) {
            for (int k = 0; k < 1508; k++) {
                if (i1 != k && !userSocialMatrix1.contains(i1, k) && set.contains(k) && set.contains(i1)) {
                    int countNumber = 0;
                    for (int t = 0; t < 1508; t++) {
                        if (i1 != k && k != t && i1 != t && userSocialMatrix1.contains(i1, t) && userSocialMatrix1.contains(t, k) && set.contains(t)
                                && userSocialMatrix1.get(i1, t) != 0.0 && userSocialMatrix1.get(t, k) != 0.0) {
                            if (userSocialMatrix1.get(i1, t) != 0.0 && userSocialMatrix1.get(t, k) != 0.0) {
                                countNumber++;
                            }
//
//                            dataTable1.put(i, k, 0.7);
//                            colMap1.put(k, i);
//
//                            break;
                        }
                    }
                    if (countNumber == 1) {
                        h1++;
//                        dataTable1.put(i, k, 0.7);
//                        colMap1.put(k, i);
                    }
                    if (countNumber >= 2) {
                        h2++;
                        // TODO 这里需要改回来
                        dataTable1.put(i1, k, 0.8);
                        colMap1.put(k, i1);
//                        dataTable2.put(i1, k, 0.7);
//                        colMap2.put(k, i1);
                    }
                }
            }

        }
        System.out.println("h1:" + h1);
        System.out.println("h2:" + h2);
        userSocialMatrix2 = new SparseMatrix(numRows, numCols, dataTable1, colMap1);
//        System.out.println(userSocialMatrix2);
        int countsum = 0;
        int countsum2 = 0;
        int s1 = 0;
        int s2 = 0;
        double[][] p1 = new double[1508][1508];
        double[][] p2 = new double[1508][1508];
        double[][] p3 = new double[1508][1508];
//        for (int userIdx = 0; userIdx < 1508; userIdx++) {
//            for (int userIdj = 0; userIdj < 1508; userIdj++) {
////                for (int item = 0; item < 3000; item++) {
////
////                }
//                double sumWeight = 0.0;
//                double sumSocial = 0.0;
//                if (!userSocialMatrix2.contains(userIdx, userIdj)) {
//                    for (int temp = 0; temp < 1508; temp++) {
//                        if (userSocialMatrix2.contains(userIdx, temp)
//                                && userSocialMatrix2.contains(temp, userIdj)
//                                && userSocialMatrix2.get(userIdx, temp) > 0
//                                && userSocialMatrix2.get(temp, userIdj) > 0) {
//                            sumWeight += userSocialMatrix2.get(userIdx, temp);
//                            sumSocial += userSocialMatrix2.get(userIdx, temp)
//                                    * userSocialMatrix2.get(temp, userIdj) * 0.667;
//                            p3[userIdx][userIdj]++;
//                        }
//                    }
////                    System.out.print(sumWeight + " " + sumSocial);
//////                    System.out.print(userIdx + " " + userIdj + " " + sumSocial / sumWeight);
////                    System.out.println();
//                    if (sumSocial > 0 && sumWeight > 0) {
//                        dataTable2.put(userIdx, userIdj, sumSocial / sumWeight);
//                        colMap2.put(userIdj, userIdx);
//                    }
//                }
//            }
//        }

        for (int i2 = 0; i2 < 1508; i2++) {
            for (int k = 0; k < 1508; k++) {
                if (i2 != k && userSocialMatrix2.contains(i2, k) && userSocialMatrix2.get(i2, k) > 0) {
//                    p1[i][k] += userSocialMatrix2.get(i,k)*userSocialMatrix2.get(k,k1);
//                    double sumWeight = 0.0;
                    for (int k1 = 0; k1 < 1508; k1++) {
                        if (k1 != k && userSocialMatrix2.contains(k, k1) && userSocialMatrix2.get(k, k1) > 0) {
//                            sumWeight += userSocialMatrix2.get(i2, k);
                            p1[i2][k1] += userSocialMatrix2.get(i2, k) * userSocialMatrix2.get(k, k1) * 0.667;
                            p2[i2][k1]++;
                            for (int k2 = 0; k2 < 1508; k2++) {
                                if (k2 != k1 && userSocialMatrix2.contains(k1, k2) && userSocialMatrix2.get(k1, k2) > 0) {
                                    p1[i2][k2] += userSocialMatrix2.get(i2, k) * userSocialMatrix2.get(k, k1)
                                            * userSocialMatrix2.get(k1, k2) * 0.333;
                                    p2[i2][k2]++;
                                }
                            }
                        }
                    }

                }
//                if(i!=k && !userSocialMatrix2.contains(i,k)&&set.contains(k) && set.contains(i)) {
//                    int countNumber = 0;
//                    double sum = 0.0;
//                    for (int t = 0; t < 1508; t++) {
//                        if (i != k && k != t && i != t && userSocialMatrix2.contains(i, t) && userSocialMatrix2.contains(t, k)&&set.contains(t)
//                                &&userSocialMatrix2.get(i, t)!=0.0&&userSocialMatrix2.get(t, k)!=0.0){
//
//////                            System.out.println(sum);
//////                            if (sum > 1) {
//////                                dataTable1.put(i, k, 0.7);
//////                                colMap1.put(k, i);
//////                            }else{
//                            if(userSocialMatrix2.get(i, t)!=0.0&&userSocialMatrix2.get(t, k)!=0.0){
////                                if(Math.abs(userSocialMatrix2.get(i, t)- 1)  < 0.000001 &&Math.abs(userSocialMatrix2.get(t, k) - 1)  < 0.000001){
//                                countNumber++;
//                                sum += (userSocialMatrix2.get(i, t) + userSocialMatrix2.get(t, k));
//                            }
//
//
//////                                dataTable1.put(i, k, 0.7);
//////                                colMap1.put(k, i);
//////                                break;
//////                            }
////                            if(userSocialMatrix.get(i, t) != 1 ||userSocialMatrix.get(t, k) !=1){
////
////                            }
//
//                        }
//
//
////                         System.out.println(countNumber);
//
//                    }
//                    double temp11 = 0.0;
//                    double temp22 = 0.0;
//                    if(countNumber == 1) {
//                        {
//                            countsum++;
//                            double f1 = 1.0;
//                            double f2 = 0.0;
//                            if(B[k]<=0.3&&B[k]>0){//这里的B[k]是专家
//                                temp11 = f1*sum/2*0.2+f2*0.2;
//                            }else if(B[k]>0.3){
//                                temp11 = f1*sum/2*0.2+f2*0.3;
//                            }else{
//                                temp11 = sum/2*0.2;
//                            }
//
//                            dataTable1.put(i, k, temp11);
//                            colMap1.put(k, i);
//
//
//                        }
//
//
////
////                            if(Math.abs(userSocialMatrix2.get(i, t)- 1)  < 0.000001 &&Math.abs(userSocialMatrix2.get(t, k) - 1)  < 0.000001){
////                                dataTable1.put(i, k, 0.5);
////                                colMap1.put(k, i);
////                                s1++;
////                            }else{
//////                                System.out.println(userSocialMatrix2.get(i, t)+" "+userSocialMatrix2.get(t, k));
////                                if(userSocialMatrix2.get(i, t)!=0.0&&userSocialMatrix2.get(t, k)!=0.0){
////                                    Double temp = (userSocialMatrix2.get(i, t)*userSocialMatrix2.get(t, k)*1.0/2.0);
//////                                    System.out.println("temp:"+temp);
////                                    dataTable1.put(i, k, temp);
////                                    colMap1.put(k, i);
////                                    s2++;
////                                }
////
////                            }
//                    }
//                    if(countNumber >= 2){
//                        { double f1 = 0.1;
//                            double f2 = 0.9;
//                            if(B[k]<=0.3&&B[k]>0){
//                                temp22 = f1*sum/(2*countNumber)*0.4+f2*0.2;
//                            }else if(B[k]>0.3){
//                                temp22 = f1*sum/(2*countNumber)*0.4+f2*0.3;
//                            }else{
//                                temp22 = sum/(2*countNumber)*0.4;
//                            }
//                            dataTable1.put(i, k,temp22 );
//                            colMap1.put(k, i);
//                            countsum2++;
//                        }
//
//                    }
//                }
            }

        }
        for (int sA = 0; sA < 1508; sA++) {
            for (int sB = 0; sB < 1508; sB++) {
                double sumCB = 0.0;
                int countAB = 0;
                double tempAB = 0.0;
                if (p1[sA][sB] > 0 && !userSocialMatrix2.contains(sA, sB)) {

                    for (int sC = 0; sC < 1508; sC++) {
                        if (userSocialMatrix2.contains(sC, sB) && userSocialMatrix2.get(sC, sB) > 0) {
                            if (p1[sA][sC] > 0 && !userSocialMatrix2.contains(sA, sC)) {
                                sumCB += p1[sA][sC];
                            }
                            if (userSocialMatrix2.contains(sA, sC) && userSocialMatrix2.get(sA, sC) > 0) {
                                countAB++;
                                sumCB += userSocialMatrix2.get(sA, sC);
                                tempAB = userSocialMatrix2.get(sA, sC);
                            }
                        }
                    }

                    double temp = 0.0;
                    if (sumCB > 0) {
                        temp = p1[sA][sB] / sumCB * 1.0;
                    }
                    if (countAB == 1) {
                        temp = temp * tempAB;
                    }
//                    if (temp > 0) {
//                        dataTable2.put(sA, sB, temp);
//                        colMap2.put(sB, sA);
//                    }

                    double temp11 = 0.0;
                    double temp22 = 0.0;
                    double all = 0.5;
                    if (p2[sA][sB] == 1) {
                        double f1 = 1.0;
                        double f3 = 0.5;
                        double d1 = 0.2;
//                        double d1 = 1.0;
                        if (B[sB] > 0 && temp > 0) {
                            temp11 = f1 * (f3 * B[sB] + (1 - f3) * global[sB]) + (1 - f1) * temp * d1;
                        } else {
                            temp11 = all * temp * d1 + (1 - all) * global[sB];
                        }
//                        dataTable1.put(sA, sB, temp11);
//                        colMap1.put(sB, sA);
                        dataTable2.put(sA, sB, temp11);
                        colMap2.put(sB, sA);
                    }
                    if (p2[sA][sB] >= 2) {
                        double f1 = 1.0;
                        double f3 = 0.5;
                        double d2 = 0.6;
//                        double d2 = 1.0;
                        if (B[sB] > 0 && temp > 0) {
                            temp22 = f1 * (f3 * B[sB] + (1 - f3) * global[sB]) + (1 - f1) * temp * d2;
                        } else {
                            temp22 = all * temp  * d2 + (1 - all) * global[sB];
                        }
//                        dataTable1.put(sA, sB, temp22);
//                        colMap1.put(sB, sA);
                        dataTable2.put(sA, sB, temp22);
                        colMap2.put(sB, sA);
                    }

                }




            }
        }
//        dataTable1.put(i, k,temp22 );
//                            colMap1.put(k, i);
        for (int sA = 0; sA < 1508; sA++) {
            for (int sB = 0; sB < 1508; sB++) {
                //尤其是在判断的时候，不能想当然，该严谨还得严谨，宁愿多加一些条件！！！深刻的教训！！！
                if (p1[sA][sB] > 0 && !userSocialMatrix2.contains(sA, sB)) {
                    double temp11 = 0.0;
                    double temp22 = 0.0;
                    double all = 0.5;
                    if (p2[sA][sB] == 1) {
                        double f1 = 1.0;
                        double f3 = 0.5;
                        double d1 = 0.2;
//                        double d1 = 1.0;
                        if (B[sB] > 0) {
                            temp11 = f1 * (f3 * B[sB] + (1 - f3) * global[sB]) + (1 - f1) * p1[sA][sB] * d1;
                        } else {
                            temp11 = all * p1[sA][sB] * d1 + (1 - all) * global[sB];
                        }
//                        dataTable1.put(sA, sB, temp11);
//                        colMap1.put(sB, sA);
//                        dataTable2.put(sA, sB, temp11);
//                        colMap2.put(sB, sA);
                    }
                    if (p2[sA][sB] >= 2) {
                        double f1 = 1.0;
                        double f3 = 0.5;
                        double d2 = 0.6;
//                        double d2 = 1.0;
                        if (B[sB] > 0) {
                            temp22 = f1 * (f3 * B[sB] + (1 - f3) * global[sB]) + (1 - f1) * p1[sA][sB] / (p2[sA][sB]) * d2;
                        } else {
                            temp22 = all * p1[sA][sB] / (p2[sA][sB]) * d2 + (1 - all) * global[sB];
                        }
//                        dataTable1.put(sA, sB, temp22);
//                        colMap1.put(sB, sA);
//                        dataTable2.put(sA, sB, temp22);
//                        colMap2.put(sB, sA);
                    }

                }
            }
        }
        System.out.println("countsum:" + countsum);
        System.out.println("countsum2:" + countsum2);
        System.out.println("s1:" + s1);
        System.out.println("s2:" + s2);


//        for(int i=0;i<numRows;i++){
//            for(int k=0;k<numCols;k++){
////                if(!userSocialMatrix.contains(i,k))
//            }
//        }
        // release memory of data table
//        System.out.print(userSocialMatrix);
//        System.out.println(userSocialMatrix.contains(51,41));
//        System.out.println(userSocialMatrix.contains(41,51));
//        System.out.println(userSocialMatrix.contains(1507,1507));
//        System.out.println(userSocialMatrix.get(6,93));
//        for(int i=0;i<1508;i++){
//            for(int k=0;k<j;k++){
//                if(userSocialMatrix.contains(E[k],i)){
////                System.out.println(userSocialMatrix.get(E[k],i));
////                    System.out.println("行:"+E[k]+"列:"+i);
//                    B[i]+=A[E[k]]*userSocialMatrix.get(E[k],i);
//                }
//
//            }
////            if(B[i]>max){
////                max = B[i];
////            }
////            if(B[i] > 0) System.out.println(B[i]);
//        }
//        for(int i = 0; i < j; i++){
//          for(int k = 0; k < 1508; k++){
//              if(!userSocialMatrix2.contains(k, E[i]) && set.contains(k) && set.contains(E[i])){
//                  dataTable1.put(k, E[i], 0.1);
//                  colMap1.put(E[i], k);
//              }
//          }
//        }
//        for(int i=0;i<1508;i++) {
//            if (B[i] > 0) {
//
//                count1++;
//                for (int k = 0; k < 1508 && k != i; k++) {
//                    if (!userSocialMatrix.contains(k, i) && set.contains(k) && set.contains(i)) {
//                        if(B[i] <= 0.3){
//                            dataTable1.put(k, i, 0.2);
//                            colMap1.put(i, k);
//                        }else {
//                            dataTable1.put(k, i, 0.3);
//                            colMap1.put(i, k);
//
//                        }
////                        dataTable1.put(k, i, B[i]);
////                        colMap1.put(i, k);
//
//
//                    }
//                }
//            }
//        }
//            if(!userSocialMatrix.contains(i,646)&&set.contains(i)){
//                dataTable1.put(i, 646, 1.0);
//                colMap1.put(646, i);
//            }
//        }
////        System.out.print(max);
        userSocialMatrix = new SparseMatrix(numRows, numCols, dataTable1, colMap1);

        impUserSocialMatrix = new SparseMatrix(numRows, numCols, dataTable2, colMap2);
//
//        System.out.print(userSocialMatrix);
        dataTable = null;
        dataTable1 = null;
        dataTable2 = null;
    }


    /**
     * Get user appender.
     *
     * @return the {@code SparseMatrix} object built by the social data.
     */
    public SparseMatrix getComprehensiveUserAppender() {
        return userSocialMatrix; // 得到综合矩阵
    }

    public SparseMatrix getUserAppender() {
        return userSocialMatrix1; // 得到原始矩阵
    }

    public SparseMatrix getImpUserAppender() {
        return impUserSocialMatrix;  // 得到隐式矩阵
    }

    /**
     * Get item appender.
     *
     * @return null
     */
    public SparseMatrix getItemAppender() {
        return null;
    }

    /**
     * Set user mapping data.
     *
     * @param userMappingData user {raw id, inner id} map
     */
    @Override
    public void setUserMappingData(BiMap<String, Integer> userMappingData) {
        this.userIds = userMappingData;
    }

    /**
     * Set item mapping data.
     *
     * @param itemMappingData item {raw id, inner id} map
     */
    @Override
    public void setItemMappingData(BiMap<String, Integer> itemMappingData) {
        this.itemIds = itemMappingData;
    }

}
