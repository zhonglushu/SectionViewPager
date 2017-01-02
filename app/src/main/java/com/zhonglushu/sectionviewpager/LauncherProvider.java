/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhonglushu.sectionviewpager;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

public class LauncherProvider {

    private static final String TAG = "Launcher.LauncherProvider";
    private static final boolean LOGD = false;

    private static final String TAG_SECTIONS = "sections";
    private static final String TAG_SECTION = "section";

    private static final String ATTR_CLASS_NAME = "className";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_LAYOUT_NAME = "layoutName";
    private static final String ATTR_SCREEN = "screen";
    private static final String ATTR_CELLX = "cellX";
    private static final String ATTR_CELLY = "cellY";
    private static final String ATTR_SPANX = "spanX";
    private static final String ATTR_SPANY = "spanY";

    /**
     * Loads the default set of favorite packages from an xml file.
     *
     * @param workspaceResourceId The specific container id of items to load
     * @param screenIds           set of screenIds which are used by the favorites
     */
    public static ArrayList<SectionInfo> loadFavoritesRecursive(
            Context context, int workspaceResourceId, ArrayList<Long> screenIds) {

        ArrayList<SectionInfo> sectionList = new ArrayList<SectionInfo>();
        if (LOGD) {
            Log.v(TAG, String.format(
                    "Loading favorites from resid=0x%08x", workspaceResourceId));
        }

        try {
            XmlResourceParser parser = context.getResources().getXml(workspaceResourceId);
            beginDocument(parser, TAG_SECTIONS);

            final int depth = parser.getDepth();
            int type;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser
                    .getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                boolean added = false;
                final String name = parser.getName();

                String packageName = getAttributeValue(parser, ATTR_PACKAGE_NAME);
                String className = getAttributeValue(parser, ATTR_CLASS_NAME);
                String layoutName = getAttributeValue(parser, ATTR_LAYOUT_NAME);
                String spanX = getAttributeValue(parser, ATTR_SPANX);
                String spanY = getAttributeValue(parser, ATTR_SPANY);

                SectionInfo sectionInfo = new SectionInfo();
                sectionInfo.packageName = packageName;
                sectionInfo.className = className;
                sectionInfo.layoutName = layoutName;
                sectionInfo.spanX = Integer.valueOf(spanX);
                sectionInfo.spanY = Integer.valueOf(spanY);

                String screen = getAttributeValue(parser, ATTR_SCREEN);
                String cellX = getAttributeValue(parser, ATTR_CELLX);
                String cellY = getAttributeValue(parser, ATTR_CELLY);

                sectionInfo.screenId = Integer.valueOf(screen);
                sectionInfo.cellX = Integer.valueOf(cellX);
                sectionInfo.cellY = Integer.valueOf(cellY);
                long screenId = Long.parseLong(screen);
                // Keep track of the set of screens which need to be
                // added to the db.
                if (!screenIds.contains(screenId)) {
                    screenIds.add(screenId);
                }
                sectionList.add(sectionInfo);
            }
        } catch (XmlPullParserException e) {
            Log.w("LauncherProvider", "Got exception parsing favorites.");
            e.printStackTrace();
        } catch (IOException e) {
            Log.w("LauncherProvider", "Got exception parsing favorites.");
            e.printStackTrace();
        } catch (RuntimeException e) {
            Log.w("LauncherProvider", "Got exception parsing favorites.");
            e.printStackTrace();
        }
        return sectionList;
    }

    private static final void beginDocument(XmlPullParser parser,
                                            String firstElementName) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            ;
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found "
                    + parser.getName() + ", expected " + firstElementName);
        }
    }

    /**
     * Return attribute value, attempting launcher-specific namespace first
     * before falling back to anonymous attribute.
     */
    private static String getAttributeValue(XmlResourceParser parser,
                                            String attribute) {
        String value = parser.getAttributeValue(
                "http://schemas.android.com/apk/res-auto/com.zhonglushu.sectionviewpager",
                attribute);
        if (value == null) {
            value = parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    /**
     * Return attribute resource value, attempting launcher-specific namespace
     * first before falling back to anonymous attribute.
     */
    private static int getAttributeResourceValue(XmlResourceParser parser,
                                                 String attribute, int defaultValue) {
        int value = parser.getAttributeResourceValue(
                "http://schemas.android.com/apk/res-auto/com.zhonglushu.sectionviewpager",
                attribute, defaultValue);
        if (value == defaultValue) {
            value = parser.getAttributeResourceValue(null, attribute,
                    defaultValue);
        }
        return value;
    }
}