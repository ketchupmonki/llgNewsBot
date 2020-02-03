#news-bot.py
#Copyright (C) 2020 Alexander Theulings, ketchupcomputing.com <alexander@theulings.com>
#
#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by
#the Free Software Foundation, either version 3 of the License, or
#(at your option) any later version.
# 
#This program is distributed in the hope that it will be useful,
#but WITHOUT ANY WARRANTY; without even the implied warranty of
#MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#GNU General Public License for more details.
# 
#You should have received a copy of the GNU General Public License
#along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
#Documentation for this file can be found at https://ketchupcomputing.com/llg-news-bot/


import feedparser
import pymysql

#rss feed sources
sources = [["http://feeds.bbci.co.uk/news/rss.xml", "BBC News"], ["http://feeds.bbci.co.uk/news/science_and_environment/rss.xml", "BBC News"], ["https://www.independent.co.uk/environment/climate-change/rss", "The Independent"], ["https://www.independent.co.uk/environment/green-living/rss", "The Independent"], ["https://www.independent.co.uk/environment/nature/rss", "The Independent"], ["https://www.theguardian.com/world/rss", "The Guardian"], ["https://www.theguardian.com/uk/environment/rss", "The Guardian"], ["https://www.theguardian.com/science/rss", "The Guardian"], ["https://www.theguardian.com/uk/technology/rss", "The Guardian"], ["https://www.theguardian.com/global-development/rss", "The Guardian"]]

#words of interest
wois = ["eco", "environment", "climate", "greenhouse","trees"]

dbHost = "hostname
dbUname = "username"
dbPass = "password"
botDBName = "database"
joomDBName = "database"
joomDBPrefix = "prefix_"
kuneTargetBoardID = 1
kuneTargetUserID = 2
kuneTargetUserName = "news-bot"

botDBCon = pymysql.connect(dbHost, dbUname, dbPass, botDBName)
botDBCur = botDBCon.cursor()

#Firstly check for new news articles that include words of interest.
for feedSrc in sources:
    #Check through each rss feed
    print("Parsing feed " + feedSrc[0] + ":")
    feed = feedparser.parse(feedSrc[0])
    print("(" + feed['feed']['title'] + ")")
    
    for itemNum in feed['entries']:
        for woi in wois:
            if woi in itemNum['title'].lower():
                #Word of interest found, check if it is in the DB already
                botDBCur.execute("SELECT id FROM articleList WHERE link=%s", itemNum['link'].lower()) 
                if len(botDBCur.fetchall()) == 0:
                    #If it isn't in our database already then we add it        
                    print("New link for DB: " + itemNum['link'])
                    botDBCur.execute("INSERT INTO articleList (link, title, source) VALUES (%s, %s, %s)", (itemNum['link'].lower(), itemNum['title'], feedSrc[1]))
                    botDBCur.execute("SELECT LAST_INSERT_ID();")
                    listID = botDBCur.fetchone()[0]
                    botDBCur.execute("INSERT INTO logs (user, subject, action) VALUES (-1, %s, 2)", listID)

botDBCon.commit()

#Secondly post any article links to the forums that have been approved for posting
#We need another two database connections - one for posting to the forum and another for updating articleList statuses and logs
logDBCon = pymysql.connect(dbHost, dbUname, dbPass, botDBName)
joomDBCon = pymysql.connect(dbHost, dbUname, dbPass, joomDBName)
logDBCur = logDBCon.cursor()
joomDBCur = joomDBCon.cursor()

botDBCur.execute("SELECT id, link, title, source FROM articleList WHERE status=3") 
rows = botDBCur.fetchall()
for row in rows:
    print("Posting article " + row[2]) 
    postSubject = row[3] + ": \"" + row[2] + "\""
    postMessage = "From " + row[3] + " - [url=\"" + row[1] + "\"]" + row[2] + ", " + row[1] + "[/url]"

    #Add the article to the forum's database
    joomDBCur.execute("INSERT INTO " + joomDBPrefix + "kunena_topics (params, posts, rating, category_id, first_post_userid, last_post_userid, first_post_guest_name, last_post_guest_name, first_post_time, last_post_time, subject, first_post_message, last_post_message) VALUES (\"\", 0, 0, %s, %s, %s, %s, %s, UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), %s, %s, %s)", (kuneTargetBoardID, kuneTargetUserID, kuneTargetUserID, kuneTargetUserName, kuneTargetUserName, postSubject, postMessage, postMessage))
    threadID = joomDBCur.lastrowid
    joomDBCur.execute("INSERT INTO " + joomDBPrefix + "kunena_messages (catid, userid, name, time, thread, subject) VALUES (%s, %s, %s, UNIX_TIMESTAMP(), %s, %s)", (kuneTargetBoardID, kuneTargetUserID, kuneTargetUserName, threadID, postSubject))
    messageID = joomDBCur.lastrowid;
    joomDBCur.execute("INSERT INTO " + joomDBPrefix + "kunena_messages_text (message, mesid) VALUES (%s, %s)", (postMessage, messageID))
    joomDBCur.execute("UPDATE " + joomDBPrefix + "kunena_topics SET first_post_id=%s, last_post_id=%s WHERE id=%s", (messageID, messageID, threadID))
    joomDBCur.execute("UPDATE " + joomDBPrefix + "kunena_categories SET numTopics=numTopics + 1, numPosts=numPosts + 1, last_topic_id=%s, last_post_id=%s WHERE id = %s", (threadID, messageID, kuneTargetBoardID))
    
    #Log this action and update the status of the article to 'posted'
    logDBCur.execute("INSERT INTO logs (user, subject, action) VALUES (-1, %s, 5)", row[0])
    logDBCur.execute("UPDATE articleList SET status=5 WHERE id=%s", row[0])
    
    logDBCon.commit()
    joomDBCon.commit()

botDBCon.close()
logDBCon.close()
joomDBCon.close()
