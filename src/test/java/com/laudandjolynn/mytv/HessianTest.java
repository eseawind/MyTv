/*******************************************************************************
 * Copyright 2015 htd0324@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.laudandjolynn.mytv;

import junit.framework.TestCase;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.caucho.hessian.client.HessianProxyFactory;
import com.laudandjolynn.mytv.service.JolynnTv;
import com.laudandjolynn.mytv.utils.Config;
import com.laudandjolynn.mytv.utils.DateUtils;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2015年3月30日 下午5:07:35
 * @copyright: www.laudandjolynn.com
 */
public class HessianTest extends TestCase {
	private final static String url = "http://" + Config.NET_CONFIG.getIp()
			+ ":" + Config.NET_CONFIG.getHessianPort() + "/epg";

	/**
	 * 测试电视台分类
	 */
	public void testEpgClassify() {
		HessianProxyFactory proxy = new HessianProxyFactory();
		try {
			JolynnTv tv = (JolynnTv) proxy.create(JolynnTv.class, url);
			String classify = tv.getMyTvClassify();
			JSONArray array = JSON.parseArray(classify);
			System.out.println(array);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 测试电视台
	 */
	public void testEpgStation() {
		HessianProxyFactory proxy = new HessianProxyFactory();
		try {
			JolynnTv tv = (JolynnTv) proxy.create(JolynnTv.class, url);
			String stations = tv.getMyTvByClassify("番禺有线");
			JSONArray array = JSON.parseArray(stations);
			System.out.println(array);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 测试电视节目
	 */
	public void testEpgProgram() {
		HessianProxyFactory proxy = new HessianProxyFactory();
		try {
			JolynnTv tv = (JolynnTv) proxy.create(JolynnTv.class, url);
			String program = tv.getProgramTable("凤凰卫视", "凤凰卫视电视台",
					DateUtils.today());
			JSONArray array = JSON.parseArray(program);
			System.out.println(array);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
