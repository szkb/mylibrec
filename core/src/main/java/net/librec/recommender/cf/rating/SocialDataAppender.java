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
package net.librec.recommender.cf.rating;

import com.google.common.collect.*;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.DataAppender;
import net.librec.math.structure.SparseMatrix;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;

/**
 * A <tt>SocialDataAppender</tt> is a class to process and store social appender
 * data.
 *
 * @author SunYatong
 */
public class SocialDataAppender extends Configured implements DataAppender {

    /** The size of the buffer */
    private static final int BSIZE = 1024 * 1024;

    /** a {@code SparseMatrix} object build by the social data */
    private SparseMatrix userSocialMatrix;

    /** The path of the appender data file */
    private String inputDataPath;

    /** User {raw id, inner id} map from rating data */
    private BiMap<String, Integer> userIds;

    /** Item {raw id, inner id} map from rating data */
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
     * @param conf  {@code Configuration} object for construction
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
     * @param inputDataPath
     *            the path of the data file
     * @throws IOException if I/O error occurs during reading
     */
    public void readData(String inputDataPath) throws IOException {

        int count1 = 0;//记录可信度的范围人数
        int count = 0;//记录专家的人数
        Double max = 0.0;
        int j= 0;
        Double[] A = new Double[1508];//保存专家的可信度
        Double[] B = new Double[1508];
        int[] hash = new int[1508];//保存每个用户被信任的次数
        int[] E = new int[1508];//保存专家对应的索引和被信任的次数
        for(int i =0;i<1508;i++){
            hash[i] = 0;
            B[i] = 0.0;
        }
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        Set<Integer> set = new HashSet<Integer>();






        // Table {row-id, col-id, rate}
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Table<Integer, Integer, Double> dataTable1 = HashBasedTable.create();
        // Map {col-id, multiple row-id}: used to fast build a rating matrix
        Multimap<Integer, Integer> colMap = HashMultimap.create();
        Multimap<Integer, Integer> colMap1 = HashMultimap.create();
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
        //用一个map来存储用户所获得的信任次数
        for(int i = 0;i<1508;i++){
            if(hash[i]>0){
                map.put(i,hash[i]);
            }
        }
        //运用Collections来对map进行排序
        List<Entry<Integer, Integer>> list = new ArrayList<Entry<Integer, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Entry<Integer, Integer>>() {
            public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
                return (o2.getValue() - o1.getValue());
            }
        });

        for(Entry<Integer, Integer> t:list){
            if(count>45) break;
            count++;
            E[j++]=t.getKey();
            A[t.getKey()]=t.getValue()*1.00/45;

//            System.out.println(t.getKey()+":"+t.getValue());
        }
        Arrays.sort(hash);
        for(int i =hash.length-1;i>=0;i--){
            if(count>45) break;
            if(hash[i]!=0) {
                count++;
                System.out.println(i);
                E[j++]=i;
                A[i]= hash[i]*1.00/45;
//                System.out.println(A[i]);
            }
        }
//        Arrays.sort(hash);
//        System.out.println(hash.length);
//        System.out.println("************");
//        System.out.println(hash[hash.length-1]);
//        System.out.println("************");

//        for(int i =hash.length-1;i>=0;i--){
//            System.out.print(hash[i]);
//            System.out.print("************");
//        }







        int numRows = userIds.size(), numCols = userIds.size();
//        System.out.print(numRows+"\n");
//        System.out.println(numCols);
        // build rating matrix
        userSocialMatrix = new SparseMatrix(numRows, numCols, dataTable, colMap);

        for(int i=0;i<numRows;i++){
            for(int k=0;k<numCols;k++){
//                if(!userSocialMatrix.contains(i,k))
            }
        }
        // release memory of data table
//        System.out.print(userSocialMatrix);
//        System.out.println(userSocialMatrix.contains(51,41));
//        System.out.println(userSocialMatrix.contains(41,51));
//        System.out.println(userSocialMatrix.contains(1507,1507));
//        System.out.println(userSocialMatrix.get(6,93));
        for(int i=0;i<1508;i++){
            for(int k=0;k<j;k++){
                if(userSocialMatrix.contains(E[k],i)){
//                System.out.println(userSocialMatrix.get(E[k],i));
//                    System.out.println("行:"+E[k]+"列:"+i);
                    B[i]+=A[E[k]]*userSocialMatrix.get(E[k],i);
                }

            }
            if(B[i]>max){
                max = B[i];
            }
//            System.out.println(B[i]);
        }
        for(int i=0;i<1508;i++) {
            if (B[i] > 0) {

                count1++;
                for (int k = 0; k < 1508 && k != i; k++) {
                    if (!userSocialMatrix.contains(k, i) && set.contains(k) && set.contains(i)) {
                        dataTable1.put(k, i, 0.9);
                        colMap1.put(i, k);

                    }
//                    if(userSocialMatrix.contains(k,i)){
//
//                    }
//                }
                }
            }
        }
//            if(!userSocialMatrix.contains(i,646)&&set.contains(i)){
//                dataTable1.put(i, 646, 1.0);
//                colMap1.put(646, i);
//            }
//        }
////        System.out.print(max);
        userSocialMatrix = new SparseMatrix(numRows, numCols, dataTable1, colMap1);
//
////        System.out.print(userSocialMatrix);
        dataTable = null;
        dataTable1 = null;
    }

    /**
     * Get user appender.
     *
     * @return the {@code SparseMatrix} object built by the social data.
     */
    public SparseMatrix getUserAppender() {
        return userSocialMatrix;
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
     * @param userMappingData
     *            user {raw id, inner id} map
     */
    @Override
    public void setUserMappingData(BiMap<String, Integer> userMappingData) {
        this.userIds = userMappingData;
    }

    /**
     * Set item mapping data.
     *
     * @param itemMappingData
     *            item {raw id, inner id} map
     */
    @Override
    public void setItemMappingData(BiMap<String, Integer> itemMappingData) {
        this.itemIds = itemMappingData;
    }

//    public static void main(String[] args){
////        System.out.println(userSocialMatrix);
//        System.out.print("hh");
//    }

}
