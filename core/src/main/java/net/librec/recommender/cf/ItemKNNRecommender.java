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
package net.librec.recommender.cf;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.SocialRecommender;
import net.librec.util.Lists;

import java.util.*;
import java.util.Map.Entry;

/**
 * ItemKNNRecommender
 *
 * @author WangYuFeng and Keqiang Wang
 */
@ModelData({"isRanking", "knn", "itemMeans","socialMatrix", "trainMatrix", "similarityMatrix"})
public class ItemKNNRecommender extends AbstractRecommender {
    private int knn;
    private DenseVector itemMeans;
    private SymmMatrix similarityMatrix;
    private List<Map.Entry<Integer, Double>>[] itemSimilarityList;
    private int currentUserIdx = -1;
    private Set<Integer> currentItemIdxSet;

    private List<Map.Entry<Integer, Double>>[] userSimilarityList;
    private DenseVector userMeans;
    double p1 = 0.0;
    double p2 = 0.0;
    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#setup()
     */
    @Override
//    protected void setup() throws LibrecException {
    public void setup() throws LibrecException {
        super.setup();
        knn = conf.getInt("rec.neighbors.knn.number",50);
        similarityMatrix = context.getSimilarity().getSimilarityMatrix();
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#trainModel()
     */
    @Override
    protected void trainModel() throws LibrecException {
        itemMeans = new DenseVector(numItems);
        int numRates = trainMatrix.size();
        double globalMean = trainMatrix.sum() / numRates;
        for (int  itemIdx = 0; itemIdx < numItems; itemIdx++) {
            SparseVector userRatingVector = trainMatrix.column(itemIdx);
            itemMeans.set(itemIdx, userRatingVector.getCount() > 0 ? userRatingVector.mean() : globalMean);
        }
//        userMeans = new DenseVector(numUsers);
//        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
//            SparseVector userRatingVector = trainMatrix.row(userIdx);
//            userMeans.set(userIdx, userRatingVector.getCount() > 0 ? userRatingVector.mean() : globalMean);
//        }
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#predict(int, int)
     */
    public double predict(int userIdx, int itemIdx) throws LibrecException {
        //create userSimilarityList if not exists
//        if (!(null != userSimilarityList && userSimilarityList.length > 0)) {
//            createUserSimilarityList();
//        }

        //create itemSimilarityList if not exists
        if (!(null != itemSimilarityList && itemSimilarityList.length > 0)) {
            createItemSimilarityList();
        }

        if (currentUserIdx != userIdx) {
            currentItemIdxSet = trainMatrix.getColumnsSet(userIdx);
            currentUserIdx = userIdx;
        }

        // find a number of similar users
//        List<Map.Entry<Integer, Double>> nns = new ArrayList<>();
//        List<Map.Entry<Integer, Double>> simList = userSimilarityList[userIdx];

//        int count = 0;
//        Set<Integer> userSet = trainMatrix.getRowsSet(itemIdx);
//        for (Map.Entry<Integer, Double> userRatingEntry : simList) {
//            int similarUserIdx = userRatingEntry.getKey();
//            if (!userSet.contains(similarUserIdx)) {
//                continue;
//            }
//            double sim = userRatingEntry.getValue();
//            if (isRanking) {
//                nns.add(userRatingEntry);
//                count++;
//            } else if (sim > 0) {
//                nns.add(userRatingEntry);
//                count++;
//            }
//            if (count == knn) {
//                break;
//            }
//        }
//        if (nns.size() == 0) {
//            return isRanking ? 0 : globalMean;
//        }
//        if (isRanking) {
//            double sum = 0.0d;
//            for (Entry<Integer, Double> userRatingEntry : nns) {
//                sum += userRatingEntry.getValue();
//            }
//            return sum;
//        } else {
//            // for rating prediction
////            System.out.println("nns:"+nns.size());
//            double sum = 0, ws = 0;
//            double u = 1.0, v = 0.0;
//            for (Entry<Integer, Double> userRatingEntry : nns) {
//                int similarUserIdx = userRatingEntry.getKey();
//                double sim = userRatingEntry.getValue();
//                if(socialMatrix.contains(userIdx,similarUserIdx)){
//                    sim = u*socialMatrix.get(userIdx,similarUserIdx)+
//                            v*userRatingEntry.getValue();
//                }
//                double rate = trainMatrix.get(similarUserIdx, itemIdx);
//                sum += sim * (rate - userMeans.get(similarUserIdx));
//                ws += Math.abs(sim);
//            }
////            return ws > 0 ? userMeans.get(userIdx) + sum / ws : globalMean;
//            if(ws > 0){
//                ws = userMeans.get(userIdx) + sum / ws;
//                p1 = ws;
////                return p1;
//            }else {
//                ws = globalMean;
//                p1 = ws;
////                return p1;
////                System.out.println(p1);
//            }
//        }





        // find a number of similar items
        List<Map.Entry<Integer, Double>> nns1 = new ArrayList<>();
        List<Map.Entry<Integer, Double>> simList1 = itemSimilarityList[itemIdx];

        int count1 = 0;
        for (Map.Entry<Integer, Double> itemRatingEntry : simList1) {
            int similarItemIdx = itemRatingEntry.getKey();
            if (!currentItemIdxSet.contains(similarItemIdx)) {
                continue;
            }

            double sim = itemRatingEntry.getValue();
            if (isRanking) {
                nns1.add(itemRatingEntry);
                count1++;
            } else if (sim > 0) {
                nns1.add(itemRatingEntry);
                count1++;
            }
            if (count1 == knn) {
                break;
            }
        }
        if (nns1.size() == 0) {
            return isRanking ? 0 : globalMean;
        }
        if (isRanking) {
            double sum = 0.0d;
            for (Entry<Integer, Double> itemRatingEntry : nns1) {
                sum += itemRatingEntry.getValue();
            }
            return sum;
        } else {
            // for rating prediction

            double sum = 0, ws1 = 0;
            for (Entry<Integer, Double> itemRatingEntry : nns1) {
                int similarItemIdx = itemRatingEntry.getKey();
                double sim = itemRatingEntry.getValue();
                double rate = trainMatrix.get(userIdx, similarItemIdx);
                sum += sim * (rate - itemMeans.get(similarItemIdx));
                ws1 += Math.abs(sim);
            }
//            return ws > 0 ? itemMeans.get(itemIdx) + sum / ws : globalMean;
            if(ws1 > 0){
                ws1 = itemMeans.get(itemIdx) + sum / ws1;
                p2 = ws1;
            }else{
                ws1 = globalMean;
                p2 = ws1;
            }
            return p2;
        }

    }

    /**
     * Create itemSimilarityList.
     */
    public void createItemSimilarityList() {
        itemSimilarityList = new ArrayList[numItems];
        for (int itemIdx = 0; itemIdx < numItems; ++itemIdx) {
            SparseVector similarityVector = similarityMatrix.row(itemIdx);
            itemSimilarityList[itemIdx] = new ArrayList<>(similarityVector.size());
            Iterator<VectorEntry> simItr = similarityVector.iterator();
            while (simItr.hasNext()) {
                VectorEntry simVectorEntry = simItr.next();
                itemSimilarityList[itemIdx].add(new AbstractMap.SimpleImmutableEntry<>(simVectorEntry.index(), simVectorEntry.get()));
            }
            Lists.sortList(itemSimilarityList[itemIdx], true);
        }
    }

    /**
     * Create userSimilarityList.
     */
//    public void createUserSimilarityList() {
//        userSimilarityList = new ArrayList[numUsers];
//        for (int userIndex = 0; userIndex < numUsers; ++userIndex) {
//            SparseVector similarityVector = similarityMatrix.row(userIndex);
//            userSimilarityList[userIndex] = new ArrayList<>(similarityVector.size());
//            Iterator<VectorEntry> simItr = similarityVector.iterator();
//            while (simItr.hasNext()) {
//                VectorEntry simVectorEntry = simItr.next();
//                userSimilarityList[userIndex].add(new AbstractMap.SimpleImmutableEntry<>(simVectorEntry.index(), simVectorEntry.get()));
//            }
//            Lists.sortList(userSimilarityList[userIndex], true);
//        }
//    }

}
