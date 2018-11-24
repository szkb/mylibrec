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
import net.librec.data.convertor.appender.SocialDataAppender;
import net.librec.math.structure.*;
import net.librec.recommender.AbstractRecommender;
import net.librec.util.Lists;

import java.util.*;
import java.util.Map.Entry;
/**
 * UserKNNRecommender
 *
 * @author WangYuFeng and Keqiang Wang
 */
@ModelData({"isRanking", "knn", "userMeans","socialMatrix", "trainMatrix", "similarityMatrix"})
//public class UserKNNRecommender extends AbstractRecommender SocialRecommender {
public class UserKNNRecommender extends AbstractRecommender {

//    socialDataAppender.readData("../data/test/test-append-dir");
    private int knn;
    private DenseVector userMeans;
    private DenseVector itemMeans;
    private SymmMatrix similarityMatrix;
    private int currentUserIdx = -1;
    private Set<Integer> currentItemIdxSet;
    private List<Entry<Integer, Double>>[] userSimilarityList;
    private List<Entry<Integer, Double>>[] itemSimilarityList;
    protected SparseMatrix socialMatrix;

    double p1 = 0.0;
    double p2 = 0.0;


    /**
     * (non-Javadoc)
     *
     * @see AbstractRecommender#setup()
     */
    @Override
//    protected void setup() throws LibrecException {
    public void setup() throws LibrecException {
        super.setup();
        socialMatrix = ((SocialDataAppender) getDataModel().getDataAppender()).getComprehensiveUserAppender();
        knn = conf.getInt("rec.neighbors.knn.number");
        similarityMatrix = context.getSimilarity().getSimilarityMatrix();
    }

    /**
     * (non-Javadoc)
     *
     * @see AbstractRecommender#trainModel()
     */
    @Override
    protected void trainModel() throws LibrecException {

        userMeans = new DenseVector(numUsers);
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            SparseVector userRatingVector = trainMatrix.row(userIdx);
            userMeans.set(userIdx, userRatingVector.getCount() > 0 ? userRatingVector.mean() : globalMean);
        }
        itemMeans = new DenseVector(numItems);
        int numRates = trainMatrix.size();
        double globalMean = trainMatrix.sum() / numRates;
        for (int  itemIdx = 0; itemIdx < numItems; itemIdx++) {
            SparseVector userRatingVector = trainMatrix.column(itemIdx);
            itemMeans.set(itemIdx, userRatingVector.getCount() > 0 ? userRatingVector.mean() : globalMean);
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see AbstractRecommender#predict(int, int)
     */
    @Override
    public double predict(int userIdx, int itemIdx) throws LibrecException {
        //create userSimilarityList if not exists

        if (!(null != userSimilarityList && userSimilarityList.length > 0)) {
            createUserSimilarityList();
        }
        if (!(null != itemSimilarityList && itemSimilarityList.length > 0)) {
            createItemSimilarityList();
        }
        // find a number of similar users
        List<Entry<Integer, Double>> nns = new ArrayList<>();
        List<Entry<Integer, Double>> simList = userSimilarityList[userIdx];

        int count = 0;
        Set<Integer> userSet = trainMatrix.getRowsSet(itemIdx);
//        System.out.println(trainMatrix);
//
        for (Entry<Integer, Double> userRatingEntry : simList) {
            int similarUserIdx = userRatingEntry.getKey();
            if (!userSet.contains(similarUserIdx)) {
                continue;
            }
            double sim = userRatingEntry.getValue();
            if (isRanking) {
                nns.add(userRatingEntry);
                count++;
            } else if (sim > 0) {
                nns.add(userRatingEntry);
                count++;
            }
            if (count == knn) {
                break;
            }
        }
        if (nns.size() == 0) {
            return isRanking ? 0 : globalMean;
        }
        if (isRanking) {
            double sum = 0.0d;
            for (Entry<Integer, Double> userRatingEntry : nns) {
                sum += userRatingEntry.getValue();
            }
            return sum;
        } else {
            // for rating prediction
//            System.out.println(socialMatrix);
            double sum = 0, ws = 0;
            double u = 1.0, v = 0.0,w = 0.0;
            int countIndex = 0;
            Set<Integer> s = new HashSet<>();
            Set<Integer> scommon = new HashSet<>();
            for(int i = 0; i < 3000; i++){
                if(trainMatrix.get(userIdx,i)>0){
                    s.add(i);
                }

            }
//            System.out.println(s.size());
            for (Entry<Integer, Double> userRatingEntry : nns) {
                int similarUserIdx = userRatingEntry.getKey();
                Set<Integer> suser = new HashSet<>();
                int countCommon = 0;//用户交集的物品数目
                int countPeople = 0;//评价交集物品的总人数
                int countNum = 0;//总数，从改进的余弦相似度来看
                for(int i = 0; i < 3000; i++){
//                    if(trainMatrix.get(userIdx,i)>0 && trainMatrix.get(similarUserIdx,i)>0){
                    if(trainMatrix.get(similarUserIdx,i)>0) {
                        suser.add(i);
                        if(trainMatrix.get(userIdx,i)>0) {
                            countCommon++;//这里和默认的countCommon有什么区别，有个门槛值
                            scommon.add(i);
                        }
                    }

//                for(Integer tt : scommon){
//                        for(int k = 0; k < 1508; k++){
//                            if(trainMatrix.get(k,tt)>0){
//                                countNum++;
//                            }
//                        }
//                }

                }
                for(int i = 0; i < 1508; i++){
                    boolean flag = true;
                    for(Integer t : scommon){
                        if(!trainMatrix.contains(i,t)){
                            flag = false;
                            break;
                        }
                    }
                    if(flag == true) countPeople++;
                }
                countIndex++;
//                System.out.println(countCommon);
//                double sim = userRatingEntry.getValue() *countCommon/(Math.sqrt(s.size()*suser.size())* Math.log10(1508 / countPeople));
//                double sim = userRatingEntry.getValue() * countCommon / s.size()* Math.log10(1508 / countPeople);
                double sim = userRatingEntry.getValue() * countCommon/ s.size();
//                double sim = userRatingEntry.getValue() *countCommon/((s.size()+suser.size()-countCommon));
//                double sim = userRatingEntry.getValue();
//                if(countCommon > 5){
//                        sim = userRatingEntry.getValue() * countCommon / s.size();* Math.log10(1508 / countPeople)
//
//                }else{
//                        sim = userRatingEntry.getValue();
//
//                }
//                if(!socialMatrix.contains(userIdx,similarUserIdx)){
//                    socialMatrix.set(userIdx,similarUserIdx,sim);
//                }

                if(socialMatrix.contains(userIdx,similarUserIdx)&&socialMatrix.get(userIdx,similarUserIdx)!=0.0) {
//                    if(socialMatrix.get(userIdx, similarUserIdx) > userRatingEntry.getValue()){
//                        sim = socialMatrix.get(userIdx, similarUserIdx);
//                    }
//                    sim = socialMatrix.get(userIdx, similarUserIdx);
//                    if(socialMatrix.get(userIdx, similarUserIdx) > 0.8) {
//                        sim = socialMatrix.get(userIdx, similarUserIdx);
////
//                    }
//                    else{
//                    if(socialMatrix.get(userIdx, similarUserIdx) < userRatingEntry.getValue()){
//                        sim = userRatingEntry.getValue();
//                    }
//                    if(countCommon > 5){
//                        sim = u * socialMatrix.get(userIdx, similarUserIdx) +
//                                v * userRatingEntry.getValue() * countCommon / s.size() ;
//                    }else{
//                        sim = u * socialMatrix.get(userIdx, similarUserIdx) +
//                                v * userRatingEntry.getValue();
//                    }
                    //需要这个
                    if (socialMatrix.get(userIdx, similarUserIdx) - 1 < 0.000001) {
                        sim =  u * socialMatrix.get(userIdx, similarUserIdx);
                    } else {
                        HashSet<Integer> setUserIdx = new HashSet<>();
                        HashSet<Integer> tempSetUserIdx = new HashSet<>();

                        int[] temp = socialMatrix.row(userIdx).getIndex();
                        for (int i = 0; i < temp.length; i++) {
                            setUserIdx.add(temp[i]);
                        }
                        tempSetUserIdx.addAll(setUserIdx);

                        HashSet<Integer> setSimilarUserIdx = new HashSet<>();
                        temp = socialMatrix.row(similarUserIdx).getIndex();
                        for (int i = 0; i < temp.length; i++) {
                            setSimilarUserIdx.add(temp[i]);
                        }

                        setUserIdx.retainAll(setSimilarUserIdx);
                        sim =  u * socialMatrix.get(userIdx, similarUserIdx) * setUserIdx.size() / tempSetUserIdx.size()
                                + v * userRatingEntry.getValue();
                    }
//                           v * userRatingEntry.getValue()* countCommon / s.size() * Math.log10(1508 / countPeople);
//                    }
//                    sim = u * socialMatrix.get(userIdx, similarUserIdx) ;
//                            v * userRatingEntry.getValue();
                }

                double rate = trainMatrix.get(similarUserIdx, itemIdx);
                sum += sim * (rate - userMeans.get(similarUserIdx));
                ws += Math.abs(sim);
            }
//            return ws > 0 ? userMeans.get(userIdx) + sum / ws : globalMean;
            if(ws > 0){
                ws = userMeans.get(userIdx) + sum / ws;
                p1 = ws;
            }else {
                ws = globalMean;
                p1 = ws;
            }
        }
//
//
//
//
////        //****************************************************//
        if (currentUserIdx != userIdx) {
            currentItemIdxSet = trainMatrix.getColumnsSet(userIdx);
            currentUserIdx = userIdx;
        }

        // find a number of similar items
        List<Entry<Integer, Double>> nns1 = new ArrayList<>();
        List<Entry<Integer, Double>> simList1 = itemSimilarityList[itemIdx];

        int count1 = 0;
        for (Entry<Integer, Double> itemRatingEntry : simList1) {
            int similarItemIdx = itemRatingEntry.getKey();
            if (!currentItemIdxSet.contains(similarItemIdx)) {
                continue;
            }

            double sim1 = itemRatingEntry.getValue();
            if (isRanking) {
                nns1.add(itemRatingEntry);
                count1++;
            } else if (sim1 > 0) {
                nns1.add(itemRatingEntry);
                count1++;
            }
            if (count1 == knn) {
                break;
            }
        }
//        if (nns1.size() == 0) {
//            return isRanking ? 0 : globalMean;
//        }
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
//            return ws1 > 0 ? itemMeans.get(itemIdx) + sum / ws1 : globalMean;
            if(ws1 > 0){
                ws1 = itemMeans.get(itemIdx) + sum / ws1;
                p2 = ws1;
            }else{
                ws1 = globalMean;
                p2 = ws1;
            }
        }
        double parameter = 0.8;
        double pre = parameter * p1 + (1 - parameter) * p2;
        return pre;


        //create itemSimilarityList if not exists
    }

    /**
     * Create userSimilarityList.
     */
    public void createUserSimilarityList() {
        userSimilarityList = new ArrayList[numUsers];
        for (int userIndex = 0; userIndex < numUsers; ++userIndex) {
            SparseVector similarityVector = similarityMatrix.row(userIndex);
            userSimilarityList[userIndex] = new ArrayList<>(similarityVector.size());
            Iterator<VectorEntry> simItr = similarityVector.iterator();
            while (simItr.hasNext()) {
                VectorEntry simVectorEntry = simItr.next();
                userSimilarityList[userIndex].add(new AbstractMap.SimpleImmutableEntry<>(simVectorEntry.index(), simVectorEntry.get()));
            }
            Lists.sortList(userSimilarityList[userIndex], true);
        }
    }
    //
//    /**
//     * Create itemSimilarityList.
//     */
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
}
