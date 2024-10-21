package com.yz.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;



@Configuration
public class RedisUtils {
	private final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

	@Resource
	public JedisPool pools ;


	/**
	 * 判断某个键是否存在
	 */
	public  boolean exists(String key) {
		Jedis jedis = pools.getResource();
		try {
			return  jedis.exists(key);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 存对象（单key）
	 */
	public  void set(String key, Object value) {
		Jedis jedis = pools.getResource();
		try {
			jedis.set(key, JSONUtil.objToStr(value));
		} finally {
			jedis.close();
		}
	}

	/**
	 * 存对象（单key）
	 */
	public  void setString(String key, String value) {
		Jedis jedis = pools.getResource();
		try {
			jedis.set(key, value);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 存对象（单key）
	 */
	public  void setString(String key, String value,int expire) {
		Jedis jedis = pools.getResource();
		try {
			jedis.set(key, value);
			if(expire>0){
				jedis.expire(key, expire);
			}
		} finally {
			jedis.close();
		}
	}

	/**
	 * 存对象（单key）
	 */
	public  void setrange(String key, Object value) {
		Jedis jedis = pools.getResource();
		try {
			jedis.setrange(key,0, JSONUtil.objToStr(value));
		} finally {
			jedis.close();
		}
	}

	/**
	 * 存对象（单key）
	 * @param key
	 * @param value
	 * @param expire
	 */
	public  void set(String key, Object value,long expire) {
		Jedis jedis = pools.getResource();
		try {
			jedis.set(key, JSONUtil.objToStr(value));
			if(expire>0){
				jedis.expire(key, (int) expire);
			}
		} finally {
			jedis.close();
		}
	}
	/**
	 * 删除某个key值
	 * @param key
	 */
	public  void del(String key) {
		Jedis jedis = pools.getResource();
		try {
			jedis.del(key);
		} finally {
			jedis.close();
		}
	}
	
	
	/**移除元素
	 * @param key
	 * @param value
	 */
	public  void remove(String key, String value) {
	    Jedis jedis = pools.getResource();
	    try {
	        jedis.srem(key, value);
	    } finally {
	        jedis.close();
	    }
	}
	
	
	/**
	 * 删除某个key值
	 * @param key
	 */
	public  void delString(String key) {
		Jedis jedis = pools.getResource();
		try {
			jedis.del(key);
		} finally {
			jedis.close();
		}
	}
	
	
	/**
	 * 
	 * 取对象（单key）
	 * 并更新有效时间
	 * 如果有效时间小于0则不更新有效时间
	 * @param key
	 * @param clazz
	 * @param expire 有效时间
	 * @return
	 */
	public  Object get(String key, Class<?> clazz,long expire) {
		Jedis jedis = pools.getResource();
		try {
			if(expire>0){
				jedis.expire(key, (int) expire);
			}
			return JSONUtil.strToObj(jedis.get(key), clazz);
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * 
	 * 取对象（单key）
	 * @param key
	 * @return
	 */
	public  String getString(String key) {
		Jedis jedis = pools.getResource();
		try {
			return jedis.get(key);
		} finally {
			jedis.close();
		}
	}
	/**
	 * 存对象列表（单key）
	 */
	public  void setList(String key, List<?> list) {
		Jedis jedis = pools.getResource();
		try {
			Pipeline pipeline = jedis.pipelined();
			for (int i = 0; i < list.size(); i++) {
				pipeline.lset(key, i, JSONUtil.objToStr(list.get(i)));
			}
			pipeline.sync();
		} finally {
			jedis.close();
		}
	}

	/**
	 * 取对象列表（单key）
	 */
	public  List<?> getList(String key, Class<?> clazz) {
		List<Object> objList = new ArrayList<Object>();

		Jedis jedis = pools.getResource();
		try {
			List<String> strList = jedis.lrange(key, 0, -1);
			if (strList != null && strList.size() > 0) {
				for (int i = 0; i < strList.size(); i++) {
					objList.add(JSONUtil.strToObj(strList.get(i), clazz));
				}
			}
		} finally {
			jedis.close();
		}

		return objList;
	}


	/************************************带事务的提交方法************************************************/
	/**
	 * 获取一个redis链接并且开启事务返还该Jedis
	 * @return
	 */
	public Transaction getJedis(){
		Jedis jedis=pools.getResource();
		return jedis.multi();
	}
	/**
	 * 存放map的key值，带时效参数的,并且带事务控制的
	 * @param parentKey
	 * @param fieldKey
	 * @param obj
	 * @param expire 有效时间秒
	 */
	public  void hashSet(String parentKey, String fieldKey, Object obj, int expire, Transaction transaction) {
		transaction.hset(parentKey, fieldKey, JSONParser.getString(obj));
		if(expire>0){
			transaction.expire(parentKey, expire);
		}
	}
	/**
	 * 删除一个map值
	 * @param parentKey
	 * @param fieldKey
	 */
	public  void hashDel(String parentKey, String fieldKey, Transaction transaction){
		transaction.hdel(parentKey, fieldKey);
	}

	public  void exec(Transaction transaction){
		transaction.exec();
	}
	/************************************************************************************/
	/**
	 * hashMap存值方式
	 * @param parentKey
	 * @param fieldKey
	 * @param obj
	 */
	public  void hashSet(String parentKey, String fieldKey, Object obj) {
		 Jedis jedis=pools.getResource();
		try{
			jedis.hset(parentKey, fieldKey,JSONParser.getString(obj));
		}finally{
			jedis.close();
		}
		
	}
	
	/**
	 * 存放map的key值，带时效参数的
	 * @param parentKey
	 * @param fieldKey
	 * @param obj
	 * @param expire 有效时间秒
	 */
	public  void hashSet(String parentKey, String fieldKey, Object obj,int expire) {
		 Jedis jedis=pools.getResource();
		try{
			jedis.hset(parentKey, fieldKey,JSONParser.getString(obj));
			if(expire>0){
				jedis.expire(parentKey, expire);
			}
			
		}finally{
			jedis.close();
		}
	}
	
	/**
	 * 获取java对象Map值
	 * @param fieldKey
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public  <T> Map<String, T> hashGetAllJavaObj(String fieldKey, Class<?> clazz) {
		Jedis jedis=pools.getResource();
		try{
			Map<String, String> map = jedis.hgetAll(fieldKey);
			if(map == null || map.size() == 0){
				return null;
			}
			
			Map<String, T> result = new HashMap<String, T>();
			for(String key : map.keySet()){
				result.put(key, (T) JSONUtil.strToObj(map.get(key), clazz));
			}
			return result;
		}finally{
			jedis.close();
		}
	}
	
	/**
	 * 取对象列表（单key）
	 */
	public  List<?> getListByKeyPrefix(String key, Class<?> clazz) {
		List<Object> objList = new ArrayList<>();
		Jedis jedis = pools.getResource();
		try {
			Set<String> keys = jedis.keys(key);
			if (keys.size() > 0){
				// 使用mget命令批量获取键对应的值
				List<String> strList = jedis.mget(keys.toArray(new String[0]));
				if (strList != null && strList.size() > 0) {
					for (int i = 0; i < strList.size(); i++) {
						objList.add(JSONUtil.strToObj(strList.get(i), clazz));
					}
				}
			}
			//jedis.expire(key,CACHE_TIME);
		} finally {
			jedis.close();
		}
		return objList;
	}
	
	public  Object get(String key, Class<?> clazz) {
		Jedis jedis = pools.getResource();
		try {
			return JSONUtil.strToObj(jedis.get(key), clazz);
		} finally {
			jedis.close();
		}
	}

	
	/**
	 * 删除一个map值
	 * @param parentKey
	 */
	public  void hashDelAll(String parentKey){
		 Jedis jedis=pools.getResource();
		try{
			jedis.hdel(parentKey);
		}finally{
			jedis.close();
		}
	}
	
	/**
	 * hashMap取值方式
	 * @param parentKey
	 * @param fieldKey
	 * @param clazz
	 * @return
	 */
	public  Object hashGet(String parentKey, String fieldKey, Class<?> clazz) {
		Jedis jedis= pools.getResource();
		Object reObj = null;
		try{
			String obj = jedis.hget(parentKey, fieldKey);
			if(obj!=null){
				reObj = JSONUtil.strToObj(obj, clazz);
			}
			return reObj;
		}finally{
			jedis.close();
		}
	}

	/**
	 * 存放map的key值，带时效参数的
	 * @param parentKey
	 * @param fieldKey
	 * @param obj
	 * @param expire 有效时间秒
	 */
	public  void hashSetString(String parentKey, String fieldKey, String obj,int expire) {
		Jedis jedis=pools.getResource();
		try{
			jedis.hset(parentKey, fieldKey,obj);
			if(expire>0){
				jedis.expire(parentKey, expire);
			}

		}finally{
			jedis.close();
		}
	}

	/**
	 * 删除一个map值
	 * @param parentKey
	 * @param fieldKey
	 */
	public  void hashDel(String parentKey, String fieldKey){
		 Jedis jedis=pools.getResource();
		try{
			jedis.hdel(parentKey, fieldKey);
		}finally{
			jedis.close();
		}
		
	}
	
	/**
	 * hashMap取值方式
	 * @param parentKey
	 * @param fieldKey
	 * @return
	 */
	public  Object hashGet(String parentKey, String fieldKey) {
		Jedis jedis=pools.getResource();
		try{
			return JSONParser.getObject(jedis.hget(parentKey, fieldKey));
		}finally{
			jedis.close();
		}
	}
	/**
	 * hashMap取值方式
	 * @param parentKey
	 * @param fieldKey
	 * @return
	 */
	public  String hashGetString(String parentKey, String fieldKey) {
		Jedis jedis=pools.getResource();
		try{
			return jedis.hget(parentKey, fieldKey);
		}finally{
			jedis.close();
		}
	}

	
	/**
	 * 获取map值
	 * @param fieldKey
	 * @param T
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public  <T> Map<String, T> hashGetAll(String fieldKey, Class<?> T) {
		Jedis jedis=pools.getResource();
		try{
			Map<String, String> map = jedis.hgetAll(fieldKey);
			if(map == null || map.size() == 0){
				return null;
			}
			
			Map<String, T> result = new HashMap<String, T>();
			for(String key : map.keySet()){
				result.put(key, (T)JSONParser.getObject(map.get(key)));
			}
			return result;
		}finally{
			jedis.close();
		}
	}
	/**
	 * 添加排行榜
	 * @param key
	 * @param member
	 * @param score
	 */
//	public  void addRank(String key, String member, long score) {
//		Jedis jedis=pools.getResource();
//		try {
//			deletePlayerRank(key, member);
//			jedis.zadd(key, score, member);
//		} finally {
//			jedis.close();
//		}
//	}

	/**
	 * 添加排行榜
	 * @param key
	 * @param member
	 * @param score
	 */
	public  void addRankNew(String key, String member, long score) {
		Jedis jedis=pools.getResource();
		try {
			jedis.zadd(key, score, member);
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * 添加排行榜
	 * @param key
	 * @param member
	 * @param score
	 */
	public  void addRankNewForDouble(String key, String member, double score) {
		Jedis jedis=pools.getResource();
		try {
			jedis.zadd(key, score, member);
		} finally {
			jedis.close();
		}
	}
	
	
	/**
	 * 获取从小到大的排行数据
	 * @param key
	 * @return
	 */
	public  Set<Tuple> getLastRank(String key, int start, int end){
		Jedis jedis = pools.getResource();
		try {
			return jedis.zrangeWithScores(key, start, end);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取当前页的排行玩家 大到小
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public  Set<Tuple> getRankByPage(String key, int start, int end){
		Jedis jedis = pools.getResource();
		try {
			return jedis.zrevrangeWithScores(key, start, end);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取ZSet里value元素的score的分数
	 * @param key
	 * @param value
	 * @return
	 */
	public  Double zscore(String key, String value){
		Jedis jedis = pools.getResource();
		try {
			return jedis.zscore(key, value);
		}finally {
			jedis.close();
		}
	}

	/**
	 * 获取ZSet里value元素的score的下标
	 * @param key
	 * @param value
	 * @return
	 */
	public  long zrank(String key, String value){
		long i = -1l;
		Jedis jedis = pools.getResource();
		try {
			i= jedis.zrank(key, value);
		}catch (Exception e){
			logger.info(value+"没有入排行榜");
		}finally {
			jedis.close();
		}
		return i;
	}

	public  Set<String> zrangeByScore(String key, int start, int end){
		Jedis jedis = pools.getResource();
		try {
			return jedis.zrangeByScore(key, start, end);
		} finally {
			jedis.close();
		}
	}

	/**
	 * zSet通过前缀获取数据
	 * @param prefix
	 * @return
	 */
	public Set<String> keysWithPrefix(String prefix){
		Jedis jedis =pools.getResource();
		// 获取所有匹配前缀的键
		Set<String> keysWithPrefix = jedis.keys(prefix);
		return keysWithPrefix;
	}


	
	/**
	 * 获取在排行榜中的位置
	 */
	public  int getRankValue(String key, String member) {
		Jedis jedis = pools.getResource();
		try {
			Long temp = jedis. zrevrank(key, member);
			if(null != temp) {
				return temp.intValue()+1;
			}
			return 0;
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * 删除玩家在某个排行榜
	 * @param key
	 * @param member
	 */
	public  void deletePlayerRank(String key, String member) {
		Jedis jedis = pools.getResource();
		try {
			jedis.zrem(key, member);
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * 计算排行榜中的总记录数
	 * @param key
	 * @return
	 */
//	public  long getRankTotalCount(String key) {
//		Jedis jedis = pools.getResource();
//		try {
//			return jedis.zcount(key, 0, Double.MAX_VALUE);
//		} finally {
//			jedis.close();
//		}
//	}

	/**
	 * 计算排行榜中的总记录数
	 * @param key
	 * @return
	 */
	public  long getRankTotalCount(String key) {
		Jedis jedis = pools.getResource();
		try {
			return jedis.zcard(key);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 清除排行榜
	 * @param key
	 */
	public  void clearRank(String key){
		Jedis jedis = pools.getResource();
		try {
			jedis.del(key);
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * 加锁
	 * @param key
	 * @return
	 */
	public  boolean tryLock(String key){
		Jedis jedis = pools.getResource();
		try {
			long result =jedis.setnx(key, "1");
			if(result==1){
				//加锁过后必须设置过期时间，防止死锁
				jedis.expire(key, 5);
			    return true;
			}
		} finally {
			jedis.close();
		}
	    return false;
	}


	/**
	 *  释放锁
	 * @param key
	 */
	public  void  releaseLock(String key){
		Jedis jedis = pools.getResource();
		try {
			jedis.del(key);
		} finally {
			jedis.close();
		}
	}
	
	
	/**
	 *  获取自增键值，判断键值是否存在
	 * @param key
	 */
	public  boolean  checkRepetition(String key){
		Jedis jedis = pools.getResource();
		try {
			jedis.incr("ipCount:"+key);
			jedis.expire("ipCount:"+key, 10);
			if("100".equals(jedis.get("ipCount:"+key))) {
				return false;
			}
			return true;
		}finally {
			jedis.close();
		}
		
	}
	
	/**
	 *  添加元素
	 * @param key
	 */
	public  Long sadd(String key,String value, long expire){
		Jedis jedis = pools.getResource();
		Long sadd=0L;
		try {
			sadd = jedis.sadd(key,value);
			if(expire>0){
				jedis.expire(key, (int) expire);
			}
		}finally {
			jedis.close();
		}
		return sadd;
	}
	
    /**删除元素
     * @param key
     * @param value
     */
    public  void srem(String key,String value){
    	Jedis jedis = pools.getResource();
    	try {
        	jedis.srem(key, value);
    	}finally {
			jedis.close();
		}
    }
	
	
	/**
	 * 获取SET集合元素
	 * @param key
	 * @return
	 */
	public  Set<String> smembers(String key){
		Jedis jedis = pools.getResource();
		try {
		return jedis.smembers(key);
		}finally {
			jedis.close();
		}

	}


	/**
	 * 随机返回一个SET中的数据（会删除掉数据）
	 * @param key
	 * @return
	 */
	public  String spop(String key){
		Jedis jedis = pools.getResource();
		try {
			return jedis.spop(key);
		}finally {
			jedis.close();
		}

	}

	/**
	 * 随机返回一个SET中的数据
	 * @param key
	 * @returns
	 */
	public  List<String> srandmember(String key,int count){
		Jedis jedis = pools.getResource();
		try {
			return jedis.srandmember(key,count);
		}finally {
			jedis.close();
		}

	}
	/**
	 * 判断key 中的值是否存在
	 * @param key
	 * @return
	 */
	public  Boolean sismember(String key,String member){
		Jedis jedis = pools.getResource();
		try {
		return jedis.sismember(key, member);
		}finally {
			jedis.close();
		}
		
		 
	}
	public  boolean  incr(String key){
		Jedis jedis = pools.getResource();
		try {
			jedis.incr(key);
		}finally {
			jedis.close();
		}
		
		return true;
	}

	public  boolean  incr(String key,int expire){
		Jedis jedis = pools.getResource();
		try {
			jedis.incr(key);
			if(expire>0){
				jedis.expire(key, expire);
			}
		}finally {
			jedis.close();
		}

		return true;
	}

	/**
	 * 队列中放入（左）
	 * @param key
	 * @param member
	 */
	public  void lpush(String key, String member) {
		Jedis jedis = pools.getResource();
		try {
			jedis.lpush(key, member);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 队列中放入（右）
	 * @param key
	 * @param member
	 */
	public  void rpush(String key, String member) {
		Jedis jedis = pools.getResource();
		try {
			jedis.rpush(key, member);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取key对应list下标为index的元素
	 * @param key
	 * @param index
	 */
	public  String lindex(String key, int index) {
		Jedis jedis = pools.getResource();
		try {
			return jedis.lindex(key, index);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取key对应list的长度
	 * @param key
	 */
	public  long llen(String key) {
		Jedis jedis = pools.getResource();
		try {
			return jedis.llen(key);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取队列的消息
	 * @param key
	 * @return
	 */
	public  String rpop(String key) {
		Jedis jedis = pools.getResource();
		try {
			return jedis.rpop(key);
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * 获取队列的消息阻塞
	 * @param key
	 * @return
	 * @throws InterruptedException 
	 */
	public  List<String> bLPop(String key) throws Exception {
		
		Jedis jedis = pools.getResource();
		try {
			return  jedis.brpop(3600,key);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取redis的事物
	 * @return
	 */
	public Transaction getTransaction(){
		Jedis jedis = pools.getResource();
		Transaction transaction = jedis.multi();
		return transaction;
	}
	
	
	public  void zadd(String key, double score,Object obj){
		Jedis jedis= pools.getResource();
		try{
			jedis.zadd(key, score, JSONUtil.objToStr(obj));
		}finally{
			jedis.close();
		}
	}
	
	/**获取jedis.zadd(key, score, member);里面的总数量
	 * @param key
	 * @return
	 */
	public  Long getTotalMessages(String key){
		Jedis jedis= pools.getResource();
		try {
			return jedis.zcard(key);
		} finally {
			jedis.close();
		}
	}
	/**
	 * 获取zSet中数据的总数量
	 * @return
	 */
	public long getZSetCount(String key){
		Jedis jedis = pools.getResource();
		try {
			return jedis.zcard(key);
		} finally {
			jedis.close();
		}
	}
	

	public  Set<String> zrange(String key,long start,long stop){
		Jedis jedis= pools.getResource();
		try{
			Set<String> set=jedis.zrange(key, start, stop);
			return set;
		}finally{
			jedis.close();
		}
	}
	
	
	/**
	 * 获取唯一id
	 * @return
	 */
	public  long getUniqueId(String key) {
		Jedis jedis = pools.getResource();
		try {
			String luaScript = "return redis.call('incr', KEYS[1])";
			Object result = jedis.eval(luaScript, 1, key);
			return (Long) result;
		} finally {
			jedis.close();
		}
	}
	
	
	 /**删除多个以特定前缀开头的键
	 * @param prefix
	 */
	public  void deleteKeysByPrefix(String prefix) {
		    Jedis jedis = pools.getResource();
	        try {
	            String cursor = "0";
	            ScanParams scanParams = new ScanParams().match(prefix + "*").count(100);
	            List<String> keysToDelete = new ArrayList<>();
	            do {
	                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
	                keysToDelete.addAll(scanResult.getResult());
	                cursor = scanResult.getCursor();
	            } while (!cursor.equals("0"));

	            if (!keysToDelete.isEmpty()) {
	                jedis.del(keysToDelete.toArray(new String[0]));
	            } 
	        }finally {
	        	  jedis.close();
	        }
	  }

	public  Set<String> getAllMembers(String key) {
	    Jedis jedis= pools.getResource();
		try{
			Set<String> set=jedis.zrange(key, 0, -1);
			return set;
		}finally{
			jedis.close();
		}
	}
	
	public  void zadd(String key, String member, long score, int expire) {
		Jedis jedis=pools.getResource();
		try {
			jedis.zadd(key, score, member);
			jedis.expire(key, expire);
		} finally {
			jedis.close();
		}
	}

}
