package net.librec.recommender.cf;//package net.librec.recommender.cf.rating;
//
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.convertor.appender.*;
import net.librec.math.structure.*;
import net.librec.recommender.SocialRecommender;
import net.librec.util.DriverClassUtil;
import net.librec.util.Lists;
import net.librec.util.ReflectionUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

/**
 * Created by szkb on 2018/3/13.
 */

@ModelData({"isRanking", "knn", "userMeans","socialMatrix", "trainMatrix", "similarityMatrix"})
public class Social extends SocialRecommender {
    private DenseVector userMeans;
    private List<Map.Entry<Integer, Double>>[] userSimilarityList;
    private SymmMatrix similarityMatrix;
    private int knn;

    ;
//    public Social(SparseMatrix socialMatrix){
//        this.socialMatrix = socialMatrix;
//    }
//    public void setSocialMatrix(SparseMatrix socialMatrix){
//        this.socialMatrix = socialMatrix;
//    }
@Override
//    protected void setup() throws LibrecException {
public void setup() throws LibrecException {
    conf.set("data.appender.class", "social");
}
    @Override
    protected void trainModel() throws LibrecException{
        userMeans = new DenseVector(numUsers);
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            SparseVector userRatingVector = trainMatrix.row(userIdx);
            userMeans.set(userIdx, userRatingVector.getCount() > 0 ? userRatingVector.mean() : globalMean);
        }
    }
////    @Override
////
//    public double predict(int userIdx, int itemIdx) throws LibrecException{
//        if (!(null != userSimilarityList && userSimilarityList.length > 0)) {
//            createUserSimilarityList();
//        }
//        return 0.1;
//    }
////    }
//
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
    @Test
    public void test()throws IOException, LibrecException, ClassNotFoundException{
        String inputPath =  conf.get("dfs.data.dir") + "/" + conf.get("data.appender.path");
        TextDataConvertor textDataConvertor = new TextDataConvertor(inputPath);
        textDataConvertor.processData();
        conf.set("data.appender.path", "filmtrust/trust/trust.txt");
        net.librec.data.convertor.appender.SocialDataAppender dataFeature = (net.librec.data.convertor.appender.SocialDataAppender) ReflectionUtil.newInstance(DriverClassUtil.getClass(conf.get("data.appender.class")), conf);
        dataFeature.setUserMappingData(textDataConvertor.getUserIds());
        dataFeature.processData();

        System.out.println(socialMatrix);

    }

//    public static void main(String args[]){
//        SparseMatrix socialMatrix1 = socialMatrix;
//        System.out.println(socialMatrix1);
//    }

}
