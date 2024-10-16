package com.yz.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yz.entity.PlayerDTO;
import com.yz.manager.AsynExecutorManager;
import com.yz.mapper.PlayerMapper;
import com.yz.model.Player;



@Service
public class PlayerServiceImpl extends ServiceImpl<PlayerMapper, PlayerDTO> implements IService<PlayerDTO> {
	
	@Autowired
	private PlayerMapper playerMapper;
	
	public void batchSavePlayer(List<Player> list) {
		List<PlayerDTO> li = new ArrayList<>();
		for (Player player : list) {
			PlayerDTO dto = changePlayerDTO(player);
			li.add(dto);
		}
		AsynExecutorManager.executeSqlTask(()->this.saveOrUpdateBatch(li));
	}
	
	public boolean isRepeatName(String name,long userId){
		QueryWrapper<PlayerDTO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("nickname", name);
		List<PlayerDTO> dtos = playerMapper.selectList(queryWrapper);
		if(dtos==null || dtos.isEmpty()){
			return false;
		}
		if (dtos.size() ==1 && dtos.get(0).getUid() == userId){
			return false;
		}
		return true;
	}
	
	public  Player getPlayerByNickname(String name){
		QueryWrapper<PlayerDTO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("nickname", name);
		PlayerDTO dto = playerMapper.selectOne(queryWrapper);
		if (dto==null) {
			return null;
		}
		Player player = changePlayer(dto);
		return player;
	}
	

	public void updateFieldByPlayerId(String playerId, String fieldName, String value) {
		UpdateWrapper<PlayerDTO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("player_id", playerId).set(fieldName, value);
		AsynExecutorManager.executeSqlTask(()->playerMapper.update(null, updateWrapper));
	}

	public void updateFieldByPlayerId(Player player, Map<String, String> map) {
		UpdateWrapper<PlayerDTO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("player_id", player.getPlayerId());
		Iterator<Entry<String, String>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, String> entry = iterator.next();
			String key = entry.getKey();
			String value = entry.getValue();
			updateWrapper.set(key, value);
		}
		AsynExecutorManager.executeSqlTask(()->playerMapper.update(null, updateWrapper));
	}
	
	public void updateFieldObjectByPlayerId(Player player, Map<String, Object> map) {
		UpdateWrapper<PlayerDTO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("player_id", player.getPlayerId());
		Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> entry = iterator.next();
			String key = entry.getKey();
			Object value = entry.getValue();
			updateWrapper.set(key, value);
		}
		AsynExecutorManager.executeSqlTask(()->playerMapper.update(null, updateWrapper));
	}


	/**
	 * 异步批量修改玩家的某些字段属性
	 * @param uidList
	 * @param map
	 */
	public void batchUpdateFieldObjectByPlayerId(List<Long> uidList, Map<String, Object> map) {
		UpdateWrapper<PlayerDTO> updateWrapper = new UpdateWrapper<>();
		updateWrapper.in("uid", uidList);
		Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> entry = iterator.next();
			String key = entry.getKey();
			Object value = entry.getValue();
			updateWrapper.set(key, value);
		}
		AsynExecutorManager.executeSqlTask(()->playerMapper.update(null, updateWrapper));
	}

	public void update(Player player) {
		PlayerDTO dto = changePlayerDTO(player);
		AsynExecutorManager.executeSqlTask(()->playerMapper.updateById(dto));
	}

	public void save(Player player) {
		PlayerDTO dto = changePlayerDTO(player);
		playerMapper.insert(dto);
	}

	private PlayerDTO changePlayerDTO(Player player) {
		PlayerDTO dto = new PlayerDTO();
		BeanUtils.copyProperties(player,dto);
		dto.setCreateTime(new Date(player.getCreateTime()));
		dto.setUpdateTime(new Date(player.getUpdateTime()));
		dto.setLoginTime(new Date(player.getLoginTime()));
		dto.setActivityTime(new Date(player.getActivityTime()));
		dto.setNewDayTime(new Date(player.getNewDayTime()));
		dto.setNewMonthTime(new Date(player.getNewMonthTime()));
		dto.setNewWeekTime(new Date(player.getNewWeekTime()));
		dto.setProps(JSON.toJSONString(player.getProps()));
		return dto;
	}

	private Player changePlayer(PlayerDTO playerDTO){
		Player player = new Player();
		BeanUtils.copyProperties(playerDTO,player);
		Gson gson = new Gson();
		player.setCreateTime(playerDTO.getCreateTime().getTime());
		player.setUpdateTime(playerDTO.getUpdateTime().getTime());
		player.setLoginTime(playerDTO.getLoginTime() != null ? playerDTO.getLoginTime().getTime() : 0);
		player.setActivityTime(playerDTO.getActivityTime() != null ? playerDTO.getActivityTime().getTime() : 0);
		player.setNewDayTime(playerDTO.getNewDayTime() != null ? playerDTO.getNewDayTime().getTime() : 0);
		player.setNewMonthTime(playerDTO.getNewMonthTime() != null ? playerDTO.getNewMonthTime().getTime() : 0);
		player.setNewWeekTime(playerDTO.getNewWeekTime() != null ? playerDTO.getNewWeekTime().getTime() : 0);
		
		if(playerDTO.getProps()!=null){
			Type propType = new TypeToken<ConcurrentHashMap<String, Long>>(){}.getType();
			ConcurrentHashMap<String, Long> prop = gson.fromJson(playerDTO.getProps(), propType);
			player.setProps(prop);
		}
		return player;
	}
	

	public Player getPlayerByUid(long uid) {
		PlayerDTO vo = this.getById(uid);
		if (vo == null) {
			return null;
		}
		Player player = changePlayer(vo);
		return player;
	}
	
	public Map<Long,Player> getPlayerMap(List<Long> uids){
		Map<Long,Player> map =new HashMap<>();
		List<PlayerDTO> players = playerMapper.selectBatchIds(uids);
		if(players!=null && !players.isEmpty()){
			for(PlayerDTO dto :players){
				Player player = changePlayer(dto);
				map.put(player.getUid(), player);
			}
		}
		return map;
	}
	

	public long getTotalPlayerNum() {
		return this.count();
	}


	public Player getPlayerByUidAndServerId(long uid, int serverId) {
		QueryWrapper<PlayerDTO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("uid", uid);
		queryWrapper.eq("server_id", serverId);
		PlayerDTO dto = playerMapper.selectOne(queryWrapper);
		if(dto==null){
			return null;
		}
		Player player = changePlayer(dto);
		return player;
	}
	
	
	public Player getPlayerByOpenId(String openId) {
		QueryWrapper<PlayerDTO> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("open_id", openId);
		PlayerDTO dto = playerMapper.selectOne(queryWrapper);
		if(dto==null){
			return null;
		}
		Player player = changePlayer(dto);
		return player;
	}
	
	
	

	public long getCountByCreateTime(Date createTime) {
		QueryWrapper<PlayerDTO> queryWrapper = new QueryWrapper<>();
		queryWrapper.ge("create_time", createTime);
		Long count = playerMapper.selectCount(queryWrapper);
		return count != null ? count : 0;
	}

	public long getCountByActivityTime(Date activityTime) {
		QueryWrapper<PlayerDTO> queryWrapper = new QueryWrapper<>();
		queryWrapper.ge("activity_time", activityTime);
		Long count = playerMapper.selectCount(queryWrapper);
		return count != null ? count : 0;
	}

	public long getCountByCreateTimeAndActivityTime(Date startCreateTime, Date endCreateTime, Date activityTime) {
		QueryWrapper<PlayerDTO> queryWrapper = new QueryWrapper<>();
		queryWrapper.ge("create_time", startCreateTime);
		queryWrapper.lt("create_time", endCreateTime);
		queryWrapper.ge("activity_time", activityTime);
		Long count = playerMapper.selectCount(queryWrapper);
		return count != null ? count : 0;
	}
	public List<Player> getPlayerList( List<Long> userIds) throws Exception{
		   LambdaQueryWrapper<PlayerDTO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		   if(userIds!=null){
			   lambdaQueryWrapper.notIn(PlayerDTO::getUid, userIds) ;       // userId 不能在指定列表中
		   }
		   lambdaQueryWrapper.apply("login_time < activity_time") 
		   .orderByDesc(PlayerDTO::getActivityTime)    // 按 activity_time 降序排列
        .last("LIMIT 20");                       // 限制返回 10 条数据
	       List<PlayerDTO> list = this.list(lambdaQueryWrapper);
	       List<Player> li = new ArrayList<>();
	       if(list!=null&&!list.isEmpty()){
	    	   for(PlayerDTO playerVO:list){
	    		   Player player = changePlayer(playerVO);
	    		   li.add(player);
		       }
	       }
	       return li;
	}
	
	public List<Player> getRandomPlayerList(List<Long> userIds) throws Exception{
		   LambdaQueryWrapper<PlayerDTO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		   if(userIds!=null){
			   lambdaQueryWrapper.notIn(PlayerDTO::getUid, userIds) ;       // userId 不能在指定列表中
		   }
		   lambdaQueryWrapper.last("ORDER BY RAND() LIMIT 10");          // 随机排序并限制返回 10 条数据
	       List<PlayerDTO> list = this.list(lambdaQueryWrapper);
	       List<Player> li = new ArrayList<>();
	       if(list!=null&&!list.isEmpty()){
	    	   for(PlayerDTO playerVO:list){
	    		   Player player = changePlayer(playerVO);
	    		   li.add(player);
		       }
	       }
	       return li;
	}



}
