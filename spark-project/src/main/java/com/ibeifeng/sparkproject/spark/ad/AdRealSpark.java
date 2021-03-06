package com.ibeifeng.sparkproject.spark.ad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;

import com.luoxuehuan.sparkproject.dao.IAdBlacklistDAO;
import com.luoxuehuan.sparkproject.dao.factory.DAOFactory;
import com.luoxuehuan.sparkproject.domain.AdBlacklist;

import kafka.serializer.StringDecoder;
import scala.Tuple2;

public class AdRealSpark {

	public static void main(String[] args) {
		SparkConf conf = new SparkConf();
		conf.setAppName("adreal");
		conf.setMaster("local[2]");//Streaming至少 2个core!
		conf.set("spark.serizlizer", "org.apache.spark.serializer.KryoSerializer");
		conf.set("spark.streaming.blockInterval", "50");
		conf.set("spark.streaming.receiver.writeAheadLog.enable", "true");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		
		/**
		 * streamingcontext！
		 * 必须有时间间隔
		 */
		JavaStreamingContext jssc = new JavaStreamingContext(conf,new Duration(5));
		
		
		Map<String,String> kafkaParam = new HashMap<String,String>();
		kafkaParam.put("metadata.borker.list", "10.127.24.21:8080");
		
		
		Set<String> topic  = new HashSet<String>();
		
		/**
		 * 数据源
		 * jssc
		 * param key class
		 * value class
		 * 
		 * class 解码 Decoder   编码 Encoder
		 * class 解码类
		 * 
		 * param
		 * 
		 * topic
		 */
		JavaPairInputDStream<String, String>  InputLogDStream = KafkaUtils.createDirectStream(
				jssc, 
				String.class, 
				String.class, 
				StringDecoder.class, 
				StringDecoder.class, 
				kafkaParam, topic);
		
		
		/**
		 * 处理log日志，转化成 想要的格式。
		 * 
		 * 使用transform转换操作! 
		 * 
		 * log日志包含什么?  
		 * 
		 * <offset,log>
		 * 
		 * 
		 * <userid,log>
		 */
		InputLogDStream.transformToPair(new Function<JavaPairRDD<String,String>, JavaPairRDD<String,String>>() {

			@Override
			public JavaPairRDD<String, String> call(JavaPairRDD<String, String> rdd) throws Exception {
				
				rdd.mapToPair(new PairFunction<Tuple2<String,String>, Long, String>() {

					@Override
					public Tuple2<Long, String> call(Tuple2<String, String> tuple)
							throws Exception {
						String log = tuple._2;
						long userid = 1L;
						return new Tuple2<Long,String>(userid,log);
					}
				});
				return null;
			}
		});
		//JavaStreamingContext, Class<K>, Class<V>, Class<KD>, Class<VD>, Map<String,String>, Set<String>) 
		//JavaStreamingContext, Class<String>, Class<String>, Class<StringEncoder>, Class<StringEncoder>, Map<String,String>, Set<String>)
	
		/**
		 * 从数据库里面获取数据, 组装成RDD,进行
		 */
		IAdBlacklistDAO adBlocklistDAO = DAOFactory.getAdBlacklistDAO();
		List<AdBlacklist> adBlacklists = adBlocklistDAO.findAll();
		
		/**
		 * list 和rdd不能直接join  通过join optional 
		 *  但是 list 转化成tuple 也不是简单的tuple ,是带true的tuple
		 * 把list 转化成rdd.
		 */
		List<Tuple2<Long,Boolean>> tuples = new ArrayList<Tuple2<Long,Boolean>>();
		
		
	}

}
