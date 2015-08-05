package edu.bupt.soft;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import org.ansj.test.WordSegAnsj;

import edu.bupt.jdbc.JDBCConnect;
import edu.bupt.jdbc.SelectOperation;


/**
 * 文本观点倾向的定量计算算法
 * @author zjd
 */
public class OrientationCompute {
	
	private static final double BETA = 0.5;                      //算法2的加权系数BETA>=0
    private static final int BASEWORD_COUNT = 40;                //褒/贬基准词表词数
    public static ArrayList<SentimentWordItem> sentimentWords;   //情感词库词语
    public static ArrayList<BaseWordItem> positiveWords;         //基准褒义词词语
    public static ArrayList<BaseWordItem> negativeWords;         //基准贬义词词语
    
    //加载词库，只需要一次
    static{
    	 loadEmotionDic();       //加载情感词库
    	 loadPositiveWords();    //加载褒义基准词词库
    	 loadNegativeWords();    //加载贬义基准词词库
    }
    
    /**
     * 加载情感词库
     */
    private static void loadEmotionDic(){
    	String sql1 = "select * from emotion_dictionary";
		ResultSet rs1 = SelectOperation.selectOnes(sql1);
		sentimentWords = new SentenceProcessor().getSentimentWords(rs1);          //获得情感词库中情感单词
    }
    
    /**
     * 加载褒义基准词词库
     */
    private static void loadPositiveWords(){
    	String sql2 = "select * from positive_baseword";
		ResultSet rs2 = SelectOperation.selectOnes(sql2);
		positiveWords =  new SentenceProcessor().getBaseWords(rs2);                   //获得褒义基准词库中单词

    }
    
    /**
     * 加载贬义基准词词库
     */
    private static void loadNegativeWords(){
    	String sql3 = "select * from negative_baseword";
		ResultSet rs3 = SelectOperation.selectOnes(sql3);
		negativeWords =  new SentenceProcessor().getBaseWords(rs3);      //获得贬义基准词库中单词

    }

    //TODO 观点句倾向值采用一种方法计算
	/**
	 * 计算观点句句子的倾向值Degree of Sentiment Orientation，把所有的情感词汇抽出，根据情感词汇的平均值计算
	 * @param sentence         输入句子
	 * @param sentimentWords   情感词表
	 * @return                 返回该句子的情感倾向值，范围为[-1,1]
	 */
	public double calcDSOofSentence(String sentence,ArrayList<SentimentWordItem> sentimentWords){
		double score = 0;      //句子的情感总分
		int wordCount = 0;     //记录句子中情感词的个数
		double result = 0;     //句子最终情感分数
		for(int i=0;i<sentimentWords.size();i++){
			if(sentence.contains(sentimentWords.get(i).getPhrase())){
				if(sentimentWords.get(i).getPolar()==1||sentimentWords.get(i).getPolar()==0){
				score += sentimentWords.get(i).getPower()/9;	                                    //该词语为褒义1或中性0
				}        
				if(sentimentWords.get(i).getPolar()==2||sentimentWords.get(i).getPolar()==0){       //该词语为贬义2或中性0
				score -= sentimentWords.get(i).getPower()/9;	                                    //负数表示极性反转
				}
				wordCount +=1;
			}
		}
		if(wordCount!=0){
			result = score/wordCount;
		}
		return result;
	}

	//TODO 非观点句句子倾向值采用三种方法计算
	/**
	 * 计算非观点句句子的倾向值Degree of Sentiment Orientation，原始未改进算法
	 * @param sentence           输入句子
	 * @param positiveWords      褒义基准词表
	 * @param negativeWords      贬义基准词表
	 * @return                   返回该句子的情感倾向值，范围为[-1,1]
	 */
	public double calcDSOofSentence(String sentence,ArrayList<BaseWordItem> positiveWords,ArrayList<BaseWordItem> negativeWords){
		Vector<String> wordSeg = WordSegAnsj.split(sentence);     //使用Ansj进行分词
		double score = 0;    //句子的情感总分
		double result = 0;   //句子最终情感分数
		if(wordSeg.size()!=0){
			try {
				for(int i=0;i<wordSeg.size();i++){
					score += new HowNetSimilarity().getWordDSO(wordSeg.get(i), positiveWords, negativeWords);
				}
			} catch (Exception e) {
				// TODO: handle exception
				score = 0;
			}
			result = score/wordSeg.size();
		}else{
			result = new HowNetSimilarity().getWordDSO(sentence, positiveWords, negativeWords);
		}	
		return result;
	}
	
	/**
	 * 计算非观点句句子的倾向值Degree of Sentiment Orientation，对应词语情感值改进算法1
	 * @param sentence           输入句子
	 * @param positiveWords      褒义基准词表
	 * @param negativeWords      贬义基准词表
	 * @return                   返回该句子的情感倾向值，范围为[-1,1]
	 */
	public double calcDSOofSentence1(String sentence,ArrayList<BaseWordItem> positiveWords,ArrayList<BaseWordItem> negativeWords){
		Vector<String> wordSeg = WordSegAnsj.split(sentence);
		double score = 0;    //句子的情感总分
		double result = 0;   //句子最终情感分数
		if(wordSeg.size()!=0){
			try {
				for(int i=0;i<wordSeg.size();i++){
					score += new HowNetSimilarity().getWordDSO1(wordSeg.get(i), positiveWords, negativeWords);
				}
			} catch (Exception e) {
				// TODO: handle exception
				score = 0;
			}
			result = score/wordSeg.size();
		}
		else{
			result = new HowNetSimilarity().getWordDSO1(sentence, positiveWords, negativeWords);
		}
		
		return result;
	}
	
	
	/**
	 * 计算非观点句句子的倾向值Degree of Sentiment Orientation，对应词语情感值改进算法2
	 * @param sentence           输入句子
	 * @param sentimentWords     情感词词表
	 * @return                   返回该句子的情感倾向值，范围为[-1,1]
	 */
	public double calcDSOofSentence2(String sentence,ArrayList<SentimentWordItem> sentimentWords){
		Vector<String> wordSeg = WordSegAnsj.split(sentence);
		double score = 0;    //句子的情感总分
		double result = 0;   //句子最终情感分数
		if(wordSeg.size()!=0){
			try {
				for(int i=0;i<wordSeg.size();i++){
					score += new HowNetSimilarity().getWordDSO2(wordSeg.get(i),sentimentWords);
				}
			} catch (Exception e) {
				// TODO: handle exception
				score = 0;
			}
			result = score/wordSeg.size();
		}else{
			result = new HowNetSimilarity().getWordDSO2(sentence,sentimentWords);
		}
		return result;
	}

	/**
	 * 计算非观点句句子的倾向值Degree of Sentiment Orientation，对应词语情感值计算的平均算法
	 * @param sentence           输入句子
	 * @param sentimentWords     情感词词表
	 * @param positiveWords      褒义基准词表
	 * @param negativeWords      贬义基准词表
	 * @return                   返回该句子的情感倾向值，范围为[-1,1]
	 */
	public double calcDSOofSentence3(String sentence,ArrayList<SentimentWordItem> sentimentWords,ArrayList<BaseWordItem> positiveWords,ArrayList<BaseWordItem> negativeWords){
		Vector<String> wordSeg = WordSegAnsj.split(sentence);
		double score = 0;      //句子的情感总分
		double result = 0;    
		if(wordSeg.size()!=0){
			try {
				for(int i=0;i<wordSeg.size();i++){
					score += new HowNetSimilarity().getWordAvgDSO(wordSeg.get(i), positiveWords, negativeWords, sentimentWords);
				}
			} catch (Exception e) {
				// TODO: handle exception
				score = 0;
			}
			result = score/wordSeg.size();
		}else{
			result = new HowNetSimilarity().getWordAvgDSO(sentence, positiveWords, negativeWords, sentimentWords);  //此时一个sentence就是一个词
		}
		return result;
	}
	
	
	/**
	 * @param sentence1          待计算的句子1
	 * @param sentence2          待计算的句子2
	 * @return              返回两个句子距离，即相似度值
	 * @throws Exception
	 */
	public double sentenceDistance(String sentence1,String sentence2) throws Exception{
		Vector<String> str1 = WordSegAnsj.split(sentence1);     //对句子1分词
		Vector<String> str2 = WordSegAnsj.split(sentence2);     //对句子2分词
		double distance = SentSimilarity.getSimilarity(str1, str2);
		return distance;
	}
	
	
	/**
	 * 计算文本的倾向值算法1
	 * @param blogContent  输入文本内容
	 * @return             文本情感倾向值
	 */
	public double calcDSOofBlog1(String blogContent){
		//获得情感词库，调用SentenceProcessor的getSentimentSentences()方法
		double value = 0;
		double score = 0;   //最后文本内容的情感倾向值
		String sql1 = "select * from emotion_dictionary";
		ResultSet rs1 = SelectOperation.selectOnes(sql1);
		ArrayList<String> sentenceList = new SentenceProcessor().SplitToSentences(blogContent);                //将文本内容分句
		ArrayList<SentimentWordItem> sentimentWords = new SentenceProcessor().getSentimentWords(rs1);          //获得情感词库中情感单词
		ArrayList<String> sentimentList = new ArrayList<String>();
		sentimentList = new SentenceProcessor().getSentimentSentences(sentenceList, sentimentWords);           //获得文本内容中观点句子
		
		//计算每个观点句子的倾向值，调用calcDSOofSentences()方法
		if(sentimentList.size()!=0){
			for(int i = 0;i < sentimentList.size();i++){
				 value += new OrientationCompute().calcDSOofSentence(sentimentList.get(i), sentimentWords);
			}
			score = value/sentimentList.size();
		}else{
			score = 0;     //文本中若无观点句，则返回情感值为0
		}
		//所有观点句子的平均观点倾向值作为文本内容的倾向值
		return score;
	}
	
	
	/**
	 * 计算文本的倾向值算法2
	 * @param blogContent  输入文本内容
	 * @return             文本情感倾向值
	 * @throws Exception 
	 */
	public double calcDSOofBlog2(String blogContent) throws Exception{
		//获得情感词库，调用SentenceProcessor的getSentimentSentences()方法
		double score = 0;          //观点句子的情感总分
		double result = 0;         //观点句子的情感平均分
		ArrayList<String> sentenceList = new SentenceProcessor().SplitToSentences(blogContent);                //将文本内容分句
	
		ArrayList<String> sentimentList = new ArrayList<String>();
		ArrayList<String> nonSentimentList = new ArrayList<String>();
		sentimentList = new SentenceProcessor().getSentimentSentences(sentenceList, sentimentWords);            //获得博客内容中观点句子
		nonSentimentList = new SentenceProcessor().getNonSentimentSentences(sentenceList, sentimentWords);      //获得博客内容中非观点句子
		

		//文本倾向值算法2的主体实现
		if(sentimentList.size()==0){                    //文本中无观点句
			if(nonSentimentList.size()!=0){
				System.out.println("case_1_nonSentiment");
				for(int i = 0;i < nonSentimentList.size();i++){
					 score += new OrientationCompute().calcDSOofSentence3(nonSentimentList.get(i), sentimentWords, positiveWords, negativeWords);
				}
				result = score/nonSentimentList.size();
			}else{
				System.out.println("case_2_NULL");
				result = 0;   //此时既没有观点句同时也没有非观点句，即为空
			}
			
		}else if(nonSentimentList.size()==0){           //文本中只有观点句，直接计算观点句情感倾向平均值作为文本情感倾向
			System.out.println("case_3_sentiment");
			for(int i = 0;i < sentimentList.size();i++){
				 score += new OrientationCompute().calcDSOofSentence(sentimentList.get(i), sentimentWords);
			}
			result = score/sentimentList.size();
		}else{
			System.out.println("case_4_sentiment&nonSentiment");
			double[] iniScoreSentiment = new double[sentimentList.size()];                                          //观点句子初始值
			double[] iniScoreNonSentiment = new double[nonSentimentList.size()];                                    //非观点句子初始值
			//计算每个观点句子的倾向值，作为观点句子初始值
			for(int i = 0;i < sentimentList.size();i++){ 
				 iniScoreSentiment[i] = new OrientationCompute().calcDSOofSentence(sentimentList.get(i), sentimentWords);			
			}
			
			//计算每个非观点句子的倾向值，作为观点句子初始值
			//TODO 词语相似度算法更改处
			for(int i = 0;i < nonSentimentList.size();i++){
				iniScoreNonSentiment[i] = new OrientationCompute().calcDSOofSentence3(nonSentimentList.get(i), sentimentWords, positiveWords, negativeWords);
			}

			for(int i = 0;i < sentimentList.size();i++){
				double value = 0;
				for(int j = 0;j < nonSentimentList.size();j++){
					double distance = new OrientationCompute().sentenceDistance(sentimentList.get(i), nonSentimentList.get(j));  //观点句与非观点句的距离
					double weight = Math.exp(BETA*distance);
					value += iniScoreNonSentiment[j] * weight;
				}
				 score += value/nonSentimentList.size();     //////////////////////////////此处处理需讨论再确定////////////////////
			}
			//所有观点句子的平均观点倾向值作为文本内容的倾向值
			result = score/sentimentList.size();	
		}	
		return result;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
	}

}