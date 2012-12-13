/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package Java.MyPet.util;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateCheck
{
    Pattern mcVersionRegex = Pattern.compile("\\[@MC:\\((\\d\\.\\d\\.\\d)\\)@\\]", Pattern.MULTILINE);
    Pattern mpVersionRegex = Pattern.compile("\\[@MP:\\((\\d\\.\\d\\.\\d)\\)@\\]", Pattern.MULTILINE);

    List<FeedItem> feedItemList = new ArrayList<FeedItem>();

    FeedItem lastAvailableUpdate = null;

    public boolean isUpdateAvailable(String mcVersion, String mpVersion)
    {
        RSSFeedParser rssFeedParser = new RSSFeedParser("http://dev.bukkit.org/server-mods/mywolf/files.rss");
        rssFeedParser.readFeed();
        lastAvailableUpdate = null;

        for (FeedItem feedItem : feedItemList)
        {
            String description = feedItem.getDescription();
            Matcher mcRegexMatcher = mcVersionRegex.matcher(description);
            while (mcRegexMatcher.find())
            {
                if (mcVersion.equals(mcRegexMatcher.group(1)))
                {
                    Matcher mpRegexMatcher = mpVersionRegex.matcher(description);

                    while (mpRegexMatcher.find())
                    {
                        if (isNewer(mpVersion, mpRegexMatcher.group(1)))
                        {
                            if (lastAvailableUpdate == null || isNewer(lastAvailableUpdate.getDate(), mpRegexMatcher.group(1)))
                            {
                                lastAvailableUpdate = feedItem;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return lastAvailableUpdate != null;
    }

    public FeedItem getLastAvailableUpdate()
    {
        return lastAvailableUpdate;
    }

    public static boolean isNewer(String oldVersion, String newVersion)
    {
        String s1 = normalisedVersion(oldVersion, ".", 4);
        String s2 = normalisedVersion(newVersion, ".", 4);
        int cmp = s1.compareTo(s2);
        return cmp < 0;
    }

    private static String normalisedVersion(String version, String sep, int maxWidth)
    {
        String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
        StringBuilder sb = new StringBuilder();
        for (String s : split)
        {
            sb.append(String.format("%" + maxWidth + 's', s));
        }
        return sb.toString();
    }

    public class FeedItem
    {
        private String title;
        private String description;
        private String date;

        public FeedItem(String title, String description, String date)
        {
            this.date = date;
            this.description = description;
            this.title = title;
        }

        public String getTitle()
        {
            return title;
        }

        public String getDescription()
        {
            return description;
        }

        public String getDate()
        {
            return date;
        }

        @Override
        public String toString()
        {
            return "FeedItem [title=" + title + ", description=" + description + ", date=" + date + "]";
        }
    }

    public class RSSFeedParser
    {
        static final String TITLE = "title";
        static final String DESCRIPTION = "description";
        static final String PUB_DATE = "pubDate";
        static final String ITEM = "item";

        final URL url;

        public RSSFeedParser(String feedUrl)
        {
            try
            {
                this.url = new URL(feedUrl);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        private String getCharacterDataFromElement(Element e)
        {
            try
            {
                Node child = e.getFirstChild();
                if (child instanceof CharacterData)
                {
                    CharacterData cd = (CharacterData) child;
                    return cd.getData();
                }
            }
            catch (Exception ignored)
            {
            }
            return "";
        }

        protected String getElementValue(Element parent, String label)
        {
            return getCharacterDataFromElement((Element) parent.getElementsByTagName(label).item(0));
        }

        public List<FeedItem> readFeed()
        {
            feedItemList.clear();

            String description;
            String title;
            String pubDate;

            try
            {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(this.url.openStream());
                NodeList nodes = doc.getElementsByTagName(ITEM);
                for (int i = 0 ; i < nodes.getLength() ; i++)
                {
                    Element element = (Element) nodes.item(i);
                    title = getElementValue(element, TITLE);
                    pubDate = getElementValue(element, PUB_DATE);
                    description = getElementValue(element, DESCRIPTION);
                    feedItemList.add(new FeedItem(title, description, pubDate));
                }
            }
            catch (Exception ignored)
            {
            }

            return feedItemList;
        }
    }
}