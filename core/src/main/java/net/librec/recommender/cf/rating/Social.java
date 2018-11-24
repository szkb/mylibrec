//package net.librec.recommender.cf.rating;
//
//import net.librec.annotation.ModelData;
//import net.librec.common.LibrecException;
//import net.librec.math.structure.DenseVector;
//import net.librec.math.structure.SparseMatrix;
//import net.librec.math.structure.SparseVector;
//import net.librec.recommender.SocialRecommender;
//import org.junit.Test;
//
///**
// * Created by szkb on 2018/3/13.
// */
//
//@ModelData({"isRanking", "knn", "userMeans","socialMatrix", "trainMatrix", "similarityMatrix"})
//public class Social extends SocialRecommender {
//    private DenseVector userMeans;
//
//    ;
////    public Social(SparseMatrix socialMatrix){
////        this.socialMatrix = socialMatrix;
////    }
////    public void setSocialMatrix(SparseMatrix socialMatrix){
////        this.socialMatrix = socialMatrix;
////    }
//@Override
////    protected void setup() throws LibrecException {
//public void setup() throws LibrecException {
//    super.setup();
////    knn = conf.getInt("rec.neighbors.knn.number");
////    similarityMatrix = context.getSimilarity().getSimilarityMatrix();
//}
////    @Override
////    protected void trainModel() throws LibrecException{
////        userMeans = new DenseVector(numUsers);
////        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
////            SparseVector userRatingVector = trainMatrix.row(userIdx);
////            userMeans.set(userIdx, userRatingVector.getCount() > 0 ? userRatingVector.mean() : globalMean);
////        }
////    }
//    @Override
//
//    public double predict(int userIdx, int itemIdx) throws LibrecException{
//        System.out.println(socialMatrix);
//        return 0.1;
//    }
//    @Test
//    public void test(){
//        System.out.println(socialMatrix);
//
//    }
//
////    public static void main(String args[]){
////        SparseMatrix socialMatrix1 = socialMatrix;
////        System.out.println(socialMatrix1);
////    }
//
//}
