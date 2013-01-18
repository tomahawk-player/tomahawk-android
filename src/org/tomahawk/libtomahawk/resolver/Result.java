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
package org.tomahawk.libtomahawk.resolver;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.Track;

import android.app.Application;
import android.graphics.drawable.Drawable;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 19.01.13
 * This class represents a result, which will be returned by a resolver.
 */
public class Result {
    private Application mApplication;
    private String mRid;
    private Collection mCollection;
    private Query mQuery;
    private ScriptResolver mScriptResolver;

    private Artist mArtist;
    private Album mAlbum;
    private Track mTrack;

    private String mPurchaseUrl;
    private String mLinkUrl;
    private String mMimeType;
    private String mFriendlySource;

    private Drawable mSourceIcon;

    private int mDuration;
    private int mBitRate;
    private int mSize;
    private int mAlbumPos;
    private int mModTime;
    private int mDiscNumber;
    private int mYear;
    private float mScore;

    private int mTrackId;
    private int mFileId;

    public Result() {
    }

    public Track getTrack() {
        return mTrack;
    }

    public void setTrack(Track mTrack) {
        this.mTrack = mTrack;
    }

    public String getPurchaseUrl() {
        return mPurchaseUrl;
    }

    public void setPurchaseUrl(String mPurchaseUrl) {
        this.mPurchaseUrl = mPurchaseUrl;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mMimeType) {
        this.mMimeType = mMimeType;
    }

    public String getLinkUrl() {
        return mLinkUrl;
    }

    public void setLinkUrl(String mLinkUrl) {
        this.mLinkUrl = mLinkUrl;
    }

    public String getFriendlySource() {
        return mFriendlySource;
    }

    public void setFriendlySource(String mFriendlySource) {
        this.mFriendlySource = mFriendlySource;
    }

    public Drawable getSourceIcon() {
        return mSourceIcon;
    }

    public void setSourceIcon(Drawable mSourceIcon) {
        this.mSourceIcon = mSourceIcon;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int mDuration) {
        this.mDuration = mDuration;
    }

    public int getBitRate() {
        return mBitRate;
    }

    public void setBitRate(int mBitRate) {
        this.mBitRate = mBitRate;
    }

    public int getSize() {
        return mSize;
    }

    public void setSize(int mSize) {
        this.mSize = mSize;
    }

    public int getAlbumPos() {
        return mAlbumPos;
    }

    public void setAlbumPos(int mAlbumPos) {
        this.mAlbumPos = mAlbumPos;
    }

    public int getModTime() {
        return mModTime;
    }

    public void setModTime(int mModTime) {
        this.mModTime = mModTime;
    }

    public int getYear() {
        return mYear;
    }

    public void setYear(int mYear) {
        this.mYear = mYear;
    }

    public int getDiscNumber() {
        return mDiscNumber;
    }

    public void setDiscNumber(int mDiscNumber) {
        this.mDiscNumber = mDiscNumber;
    }

    public float getScore() {
        return mScore;
    }

    public void setScore(float score) {
        this.mScore = score;
        if (getTrack() != null)
            getTrack().setScore(score);
    }

    public int getTrackId() {
        return mTrackId;
    }

    public void setTrackId(int mTrackId) {
        this.mTrackId = mTrackId;
    }

    public int getFileId() {
        return mFileId;
    }

    public void setFileId(int mFileId) {
        this.mFileId = mFileId;
    }

    public Artist getArtist() {
        return mArtist;
    }

    public void setArtist(Artist mArtist) {
        this.mArtist = mArtist;
    }

    public Album getAlbum() {
        return mAlbum;
    }

    public void setAlbum(Album mAlbum) {
        this.mAlbum = mAlbum;
    }

    public ScriptResolver getScriptResolver() {
        return mScriptResolver;
    }

    public void setScriptResolver(ScriptResolver mScriptResolver) {
        this.mScriptResolver = mScriptResolver;
    }

    public String getRid() {
        return mRid;
    }

    public void setRid(String mRid) {
        this.mRid = mRid;
    }

    public Collection getCollection() {
        return mCollection;
    }

    public void setCollection(Collection mCollection) {
        this.mCollection = mCollection;
    }
}
