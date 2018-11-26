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
package net.librec.recommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.data.convertor.appender.SocialDataAppender;
import net.librec.math.structure.*;
import net.librec.util.Lists;

import java.util.*;
import java.util.Map.Entry;

/**
 * Social Recommender
 *
 * @author Keqiang Wang
 */
@ModelData({"isRanking", "knn", "userMeans", "trainMatrix", "similarityMatrix"})
public abstract class SocialRecommender extends MatrixFactorizationRecommender {
    /**
     * socialMatrix: social rate matrix, indicating a user is connecting to a number of other users
     */
    protected SparseMatrix socialMatrix;

    protected SparseMatrix impSocialMatrix;

    protected SparseMatrix impTransSocialMatrix;

    /**
     * social regularization
     */
    protected float regSocial;

    protected float regImpSocial = 0.6f;

    protected float impSocialWeight = 0.8f;

    protected float socialWeight = 1.0f;

    private int knn;
    private DenseVector userMeans;
    private SymmMatrix similarityMatrix;
    private List<Map.Entry<Integer, Double>>[] userSimilarityList;

    @Override
    public void setup() throws LibrecException {
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        super.setup();
        regSocial = conf.getFloat("rec.social.regularization", 0.01f);
        // social path for the socialMatrix
        socialMatrix = ((SocialDataAppender) getDataModel().getDataAppender()).getUserAppender();
        impSocialMatrix = ((SocialDataAppender) getDataModel().getDataAppender()).getImpUserAppender();
//        impSocialMatrix = new SparseMatrix(socialMatrix);

        Table<Integer, Integer, Double> dataTableImpSocialMatrix = HashBasedTable.create();
        dataTableImpSocialMatrix = impSocialMatrix.getDataTable();

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            for (int userIdj = 0; userIdj < numUsers; userIdj++) {
                if (impSocialMatrix.contains(userIdx, userIdj) && impSocialMatrix.get(userIdx, userIdj) > 0) {
                    impSocialMatrix.set(userIdx, userIdj, socialWeight * impSocialMatrix.get(userIdx, userIdj)
                    + (1 - socialWeight) * similarity(userIdx, userIdj));
//                    dataTableImpSocialMatrix.put(userIdx, userIdj, impSocialWeight * impSocialMatrix.get(userIdx, userIdj)
//                            + (1 - impSocialWeight) * similarity(userIdx, userIdj));
                }
//                } else {
//                    if (socialMatrix.contains(userIdx, userIdj)) {
//                        dataTableImpSocialMatrix.put(userIdx, userIdj, socialWeight * socialMatrix.get(userIdx, userIdj)
//                                + (1 - socialWeight) * similarity(userIdx, userIdj));                    }
//                }
            }
        }
//        impSocialMatrix = new SparseMatrix(numUsers, numUsers, dataTableImpSocialMatrix);


        // 求方差
//        double[] Variance = new double[numUsers];
//        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
//            SparseVector itemVector = trainMatrix.row(userIdx);
//            if (itemVector.getCount() > 0) {
//                double mean = itemVector.mean();
//                double ans = 0.0;
//                for (VectorEntry vectorEntry : itemVector) {
//                    ans += Math.pow(vectorEntry.get() - mean, 2);
//                }
//                Variance[userIdx] = ans / itemVector.getCount();
//            }
//        }
//        System.out.println("**********");
//
////
////        Table<Integer, Integer, Double> dataTableImpSocialMatrix = HashBasedTable.create();
////        dataTableImpSocialMatrix = impSocialMatrix.getDataTable();
////
////        Multimap<Integer, Integer> colMapImpSocialMatrix = HashMultimap.create();
////        colMapImpSocialMatrix = impSocialMatrix.getColMap();
//
//        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
//            double tempMeanIdx = 0.0;
//            if (trainMatrix.row(userIdx).getCount() > 0) {
//                tempMeanIdx = trainMatrix.row(userIdx).mean();
//            } else {
//                continue;
//            }
//            for (int userIdj = 0; userIdj < numUsers; userIdj++) {
//                // 求均值和方差之间的关系
//
////                if (!socialMatrix.contains(userIdx, userIdj)
////                &&
//                if(!impSocialMatrix.contains(userIdx, userIdj)) {
//                    continue;
//                }
//                double pre = 0.0;
//                double tempMeanIdj = 0.0;
//                if (trainMatrix.row(userIdj).getCount() > 0) {
//                    tempMeanIdj = trainMatrix.row(userIdj).mean();
//                } else {
//                    continue;
//                }
//                double varianceIdx = Variance[userIdx];
//                double varianceIdj = Variance[userIdj];
//                double meanDifference = Math.abs(tempMeanIdx - tempMeanIdj);
//                double varianceDifference = Math.abs(Variance[userIdx] - Variance[userIdj]);
//
//                if (Math.abs(tempMeanIdx - tempMeanIdj) < 0.00001
//                        && varianceIdx != varianceIdj) {
//                    pre = Math.exp(- (meanDifference + 1) * varianceDifference);
//                } else if (Math.abs(varianceIdx - varianceIdj) < 0.00001
//                        && tempMeanIdx != tempMeanIdj) {
//                    pre = Math.exp(- meanDifference * (varianceDifference + 1));
//                } else {
//                    pre = Math.exp(- meanDifference * varianceDifference);
//                }
//
//                // 这里是要求交互的次数
//                int success = 0;
//                int failure = 0;
//                double ans = 0.0;
//                int countCommonRating = 0;
//                double similarity = 0.0;
//                for (int item = 0; item < numItems; item++) {
//                    if (trainMatrix.get(userIdx, item) > 0
//                            && trainMatrix.get(userIdj, item) > 0) {
//                        countCommonRating++;
//                        ans += Math.pow((trainMatrix.get(userIdx, item) - trainMatrix.get(userIdj, item)) / 3.5, 2);
//                    }
//
//                    if (countCommonRating > 0) {
//                        similarity = (1 - ans / countCommonRating) * (countCommonRating / (trainMatrix.row(userIdx).getCount()
//                                + trainMatrix.row(userIdj).getCount() - countCommonRating));
//                    }
//
//
//                    if ((trainMatrix.get(userIdx, item) - tempMeanIdx)
//                            * (trainMatrix.get(userIdj, item) - tempMeanIdj) >= 0) {
//                        success++;
//                    } else {
//                        failure++;
//                    }
//                }
//                double result = 0.0;
////                if (socialMatrix.contains(userIdx, userIdj) && socialMatrix.get(userIdx, userIdj) > 0) {
////                    result = similarity * pre * (failure) / (failure + success);
////                    dataTableImpSocialMatrix.put(userIdx, userIdj, result);
////                    colMapImpSocialMatrix.put(userIdj, userIdx);
////                } else {
//
//                    if (impSocialMatrix.contains(userIdx, userIdj) && impSocialMatrix.get(userIdx, userIdj) > 0) {
//                        result = impSocialMatrix.get(userIdx, userIdj)
//                                * similarity * pre * (failure) / (failure + success);
////                        dataTableImpSocialMatrix.put(userIdx, userIdj, result);
////                        colMapImpSocialMatrix.put(userIdj, userIdx);
////                        impSocialMatrix.set(userIdx, userIdj, result);
////                    }
//                }
//
//
//
//
//
//
//////                if (impSocialMatrix.contains(userIdx, userIdj)) {
//////                    System.out.println(similarity(userIdx, userIdj));
//////                    colMap.put(userIdj, userIdx);
//////                    double temp = socialWeight * impSocialMatrix.get(userIdx, userIdj)
//////                            + (1 - socialWeight) * similarity(userIdx, userIdj);
//////                    temp = temp > 1 ? 1 : temp;
//////                    dataTable.put(userIdx, userIdj, temp);
//////
//////
//////                } else {
////////                    impSocialMatrix.set(i, j, similarity(i, j)); // 弄个系数试试他们之间的关系
//////                    System.out.println(similarity(userIdx, userIdj));
//////                    dataTable.put(userIdx, userIdj, similarity(userIdx, userIdj));
//////                    colMap.put(userIdj, userIdx);
//////                }
////            }
////        }
//////        impSocialMatrix = new SparseMatrix(1508, 1508, dataTable, colMap);
////
            }
//        }
//        impSocialMatrix = new SparseMatrix(1508, 1508, dataTableImpSocialMatrix, colMapImpSocialMatrix);
//    }


//    @Override
//    protected double predict(int userIdx, int itemIdx, boolean bounded) throws LibrecException {
//        double predictRating = predict(userIdx, itemIdx);
//
//        if (bounded)
//            return denormalize(Maths.logistic(predictRating));
//
//        return predictRating;
//    }

    /**
     * denormalize a prediction to the region (minRate, maxRate)
     *
     * @param predictRating a prediction to the region (minRate, maxRate)
     * @return a denormalized prediction to the region (minRate, maxRate)
     */
    protected double denormalize(double predictRating) {
        return minRate + predictRating * (maxRate - minRate);
    }

    /**
     * normalize a rating to the region (0, 1)
     *
     * @param rating a given rating
     * @return a normalized rating
     */
    protected double normalize(double rating) {
        return (rating - minRate) / (maxRate - minRate);
    }

    // 寻找相似度
    protected double similarity(int userIdx, int userIdj) {
//        knn = conf.getInt("rec.neighbors.knn.number");
        knn = 50;
        similarityMatrix = context.getSimilarity().getSimilarityMatrix();
        if (!(null != userSimilarityList && userSimilarityList.length > 0)) {
            createUserSimilarityList();
        }
        List<Map.Entry<Integer, Double>> nns = new ArrayList<>();
        List<Map.Entry<Integer, Double>> simList = userSimilarityList[userIdx];

        int count = 0;
        // 这个的目的是相似度用户也要评价同一个项目？？
//        Set<Integer> userSet = trainMatrix.getRowsSet(itemIdx);
//        for (Map.Entry<Integer, Double> userRatingEntry : simList) {
//            int similarUserIdx = userRatingEntry.getKey();
////            if (!userSet.contains(similarUserIdx)) {
////                continue;
////            }
//            double sim = userRatingEntry.getValue();
//            if (sim > 0) {
//                nns.add(userRatingEntry);
//                count++;
//            }
//            if (count == knn) {
//                break;
//            }
//        }

        Set<Integer> s = new HashSet<>();
        for (int i = 0; i < 3000; i++) {
            if (trainMatrix.get(userIdx, i) > 0) {
                s.add(i);
            }

        }


        Entry<Integer, Double> temp = null;
        for (Entry<Integer, Double> userRatingEntry : simList) {
            if (userRatingEntry.getKey().equals(userIdj)) {
                temp = userRatingEntry;
                break;
            }
        }
        if (temp == null) {
            return 0.0;
        }
        int countCommon = 0;//用户交集的物品数目
        for (int i = 0; i < 3000; i++) {
            if (trainMatrix.get(userIdj, i) > 0) {
                if (trainMatrix.get(userIdx, i) > 0) {
                    countCommon++;//这里和默认的countCommon有什么区别，有个门槛值
                }
            }
        }
        double sim = 0.0;
        if (countCommon > 0 && s.size() > 0) {
            sim = temp.getValue() * countCommon / s.size();
        }
        return sim;
    }

    public void createUserSimilarityList() {
        userSimilarityList = new ArrayList[numUsers];
        for (int userIndex = 0; userIndex < numUsers; ++userIndex) {
            SparseVector similarityVector = similarityMatrix.row(userIndex);
            userSimilarityList[userIndex] = new ArrayList<>(similarityVector.size());
            Iterator<VectorEntry> simItr = similarityVector.iterator();
            while (simItr.hasNext()) {
                VectorEntry simVectorEntry = simItr.next();

                // 可以认为是key和value
                userSimilarityList[userIndex].add(new AbstractMap.SimpleImmutableEntry<>(simVectorEntry.index(), simVectorEntry.get()));
            }
            Lists.sortList(userSimilarityList[userIndex], true);
        }
    }

}

