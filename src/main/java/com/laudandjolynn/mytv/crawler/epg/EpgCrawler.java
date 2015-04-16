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
package com.laudandjolynn.mytv.crawler.epg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.laudandjolynn.mytv.crawler.AbstractCrawler;
import com.laudandjolynn.mytv.crawler.Parser;
import com.laudandjolynn.mytv.exception.MyTvException;
import com.laudandjolynn.mytv.model.ProgramTable;
import com.laudandjolynn.mytv.model.TvStation;
import com.laudandjolynn.mytv.service.TvService;
import com.laudandjolynn.mytv.utils.DateUtils;
import com.laudandjolynn.mytv.utils.WebCrawler;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2015年3月28日 上午12:00:44
 * @copyright: www.laudandjolynn.com
 */
class EpgCrawler extends AbstractCrawler {
	private final static Logger logger = LoggerFactory
			.getLogger(EpgCrawler.class);
	// cntv节目表地址
	private final static String EPG_URL = "http://tv.cntv.cn/epg";
	private TvService tvService = new TvService();

	public EpgCrawler(Parser parser) {
		super(parser);
	}

	/**
	 * 获取所有电视台
	 * 
	 * @return
	 */
	@Override
	public List<TvStation> crawlAllTvStation() {
		Page page = WebCrawler.crawl(EPG_URL);
		if (page.isHtmlPage()) {
			HtmlPage htmlPage = (HtmlPage) page;
			return parser.parseTvStation(htmlPage.asXml());
		}
		return null;
	}

	/**
	 * 获取指定日期的所有电视台节目表
	 * 
	 * @param date
	 *            日期，yyyy-MM-dd
	 * @return
	 */
	@Override
	public List<ProgramTable> crawlAllProgramTable(String date) {
		List<TvStation> stationList = tvService.getAllStation();
		return crawlAllProgramTable(stationList, date);
	}

	/**
	 * 根据电视台、日期获取电视节目表
	 * 
	 * @param stationName
	 *            电视台名称
	 * @param date
	 *            日期，yyyy-MM-dd
	 * @return
	 */
	@Override
	public List<ProgramTable> crawlProgramTable(String stationName, String date) {
		if (stationName == null || date == null) {
			logger.debug("station name or date is null.");
			return null;
		}
		TvStation station = tvService.getStation(stationName);
		if (station == null) {
			TvService epgService = new TvService();
			station = epgService.getStation(stationName);
		}
		return crawlProgramTable(station, date);
	}

	@Override
	public boolean crawlable(String stationName) {
		// TODO Auto-generated method stub
		return false;
	}

	private List<ProgramTable> crawlProgramTableByPage(HtmlPage htmlPage,
			TvStation station, String date) {
		if (station == null || htmlPage == null) {
			logger.debug("station and html page must not null.");
			return null;
		}
		Date dateObj = DateUtils.string2Date(date, "yyyy-MM-dd");
		if (dateObj == null) {
			logger.debug("date must not null.");
			return null;
		}
		String stationName = station.getName();
		String queryDate = DateUtils.date2String(dateObj, "yyyy-MM-dd");
		logger.info("crawl program table of " + stationName + " at "
				+ queryDate);
		TvService epgService = new TvService();
		if (epgService.isProgramTableExists(stationName, queryDate)) {
			logger.debug("the TV station's program table of " + stationName
					+ " have been saved in db.");
			return null;
		}

		String city = station.getCity();
		List<?> stationElements = null;
		if (city == null) {
			stationElements = htmlPage
					.getByXPath("//div[@class='md_left_right']/dl//h3//a[@class='channel']");
		} else {
			// 城市电视台
			stationElements = htmlPage
					.getByXPath("//dl[@id='cityList']//div[@class='lv3']//a[@class='channel']");
		}
		for (Object element : stationElements) {
			HtmlAnchor anchor = (HtmlAnchor) element;
			if (stationName.equals(anchor.getTextContent().trim())) {
				try {
					htmlPage = anchor.click();
				} catch (IOException e) {
					throw new MyTvException(
							"error occur while search program table of "
									+ stationName + " at spec date: "
									+ queryDate, e);
				}
				break;
			}
		}

		if (!queryDate.equals(DateUtils.today())) {
			DomElement element = htmlPage.getElementById("date");
			element.setAttribute("readonly", "false");
			element.setAttribute("value", queryDate);
			element.setNodeValue(queryDate);
			element.setTextContent(queryDate);
			List<?> list = htmlPage.getByXPath("//div[@id='search_1']/a");
			HtmlAnchor anchor = (HtmlAnchor) list.get(0);
			try {
				htmlPage = anchor.click();
			} catch (IOException e) {
				throw new MyTvException(
						"error occur while search program table of "
								+ stationName + " at spec date: " + queryDate,
						e);
			}
		}
		String html = htmlPage.asXml();
		List<ProgramTable> ptList = parser.parseProgramTable(html);
		return ptList;
	}

	/**
	 * 抓取所有电视台指定日志的电视节目表，多线程
	 * 
	 * @param stations
	 * @param date
	 * @return
	 */
	private List<ProgramTable> crawlAllProgramTable(List<TvStation> stations,
			final String date) {
		List<ProgramTable> resultList = new ArrayList<ProgramTable>();
		TvService epgService = new TvService();
		int threadCount = epgService.getTvStationClassify().size();
		ExecutorService executorService = Executors
				.newFixedThreadPool(threadCount);
		CompletionService<List<ProgramTable>> completionService = new ExecutorCompletionService<List<ProgramTable>>(
				executorService);
		for (final TvStation station : stations) {
			Callable<List<ProgramTable>> task = new Callable<List<ProgramTable>>() {
				@Override
				public List<ProgramTable> call() throws Exception {
					return crawlProgramTable(station.getName(), date);
				}
			};
			completionService.submit(task);
		}
		int size = stations == null ? 0 : stations.size();
		int count = 0;
		while (count < size) {
			Future<List<ProgramTable>> future;
			try {
				future = completionService.poll(5, TimeUnit.MINUTES);
				List<ProgramTable> ptList = future.get(5, TimeUnit.MINUTES);
				if (ptList != null) {
					resultList.addAll(ptList);
				}
			} catch (InterruptedException e) {
				logger.error("craw program table of all station at " + date
						+ " was interrupted.", e);
			} catch (ExecutionException e) {
				logger.error(
						"error occur while craw program table of all station at "
								+ date, e);
			} catch (TimeoutException e) {
				logger.error("query program table of all sation at at " + date
						+ " is timeout.", e);
			}
			count++;
		}
		executorService.shutdown();

		return resultList;
	}

	/**
	 * 根据电视台、日期获取电视节目表
	 * 
	 * @param station
	 *            电视台对象
	 * @param date
	 *            日期，yyyy-MM-dd
	 * @return
	 */
	private List<ProgramTable> crawlProgramTable(TvStation station, String date) {
		if (station == null) {
			logger.debug("the station must be not null.");
			return null;
		}
		Page page = WebCrawler.crawl(EPG_URL);
		if (!page.isHtmlPage()) {
			logger.debug("the page isn't html page at url " + EPG_URL);
			return null;
		}
		return crawlProgramTableByPage((HtmlPage) page, station, date);
	}
}
