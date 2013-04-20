/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.hatchet;

import java.util.ArrayList;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class ArtistInfo {

    public static String ARTISTINFO_KEY_DISAMBIGUATION = "Disambiguation";

    public static String ARTISTINFO_KEY_ID = "Id";

    public static String ARTISTINFO_KEY_IMAGES = "Images";

    public static String ARTISTINFO_KEY_MEMBERS = "Members";

    public static String ARTISTINFO_KEY_NAME = "Name";

    public static String ARTISTINFO_KEY_NAMES = "Names";

    public static String ARTISTINFO_KEY_RESOURCES = "Resources";

    public static String ARTISTINFO_KEY_URL = "Url";

    public static String ARTISTINFO_KEY_WIKIABSTRACT = "WikiAbstract";

    private String mDisambiguation;

    private String mId;

    private ArrayList<ImageInfo> mImages;

    private ArrayList<PersonInfo> mMembers;

    private String mName;

    private ArrayList<String> mNames;

    private ArrayList<ResourceInfo> mResources;

    private String mUrl;

    private String mWikiAbstract;

    public String getDisambiguation() {
        return mDisambiguation;
    }

    public void setDisambiguation(String disambiguation) {
        mDisambiguation = disambiguation;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public ArrayList<ImageInfo> getImages() {
        return mImages;
    }

    public void setImages(ArrayList<ImageInfo> images) {
        mImages = images;
    }

    public ArrayList<PersonInfo> getMembers() {
        return mMembers;
    }

    public void setMembers(ArrayList<PersonInfo> members) {
        mMembers = members;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public ArrayList<String> getNames() {
        return mNames;
    }

    public void setNames(ArrayList<String> names) {
        mNames = names;
    }

    public ArrayList<ResourceInfo> getResources() {
        return mResources;
    }

    public void setResources(ArrayList<ResourceInfo> resources) {
        mResources = resources;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getWikiAbstract() {
        return mWikiAbstract;
    }

    public void setWikiAbstract(String wikiAbstract) {
        mWikiAbstract = wikiAbstract;
    }
}
