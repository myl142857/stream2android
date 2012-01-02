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

package android.media;

import android.graphics.Bitmap;

import java.io.FileDescriptor;

/**
 * MediaMetadataRetriever class provides a unified interface for retrieving
 * frame and meta data from an input media file.
 * {@hide}
 */
public class MediaMetadataRetriever
{
    static {
        System.loadLibrary("media_jni");
    }

    public MediaMetadataRetriever() {
        native_setup();
    }
    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame close to the given time position by considering
     * the given option if possible, and returns it as a bitmap. This is
     * useful for generating a thumbnail for an input data source or just
     * obtain and display a frame at the given time position.
     *
     * @param timeUs The time position where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarantee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @param option a hint on how the frame is found. Use
     * {@link #OPTION_PREVIOUS_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp earlier than or the same as timeUs. Use
     * {@link #OPTION_NEXT_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp later than or the same as timeUs. Use
     * {@link #OPTION_CLOSEST_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp closest to or the same as timeUs. Use
     * {@link #OPTION_CLOSEST} if one wants to retrieve a frame that may
     * or may not be a sync frame but is closest to or the same as timeUs.
     * {@link #OPTION_CLOSEST} often has larger performance overhead compared
     * to the other options if there is no sync frame located at timeUs.
     *
     * @return A Bitmap containing a representative video frame, which 
     *         can be null, if such a frame cannot be retrieved.
     */
    public Bitmap getFrameAtTime(long timeUs, int option) {
        if (option < OPTION_PREVIOUS_SYNC ||
            option > OPTION_CLOSEST) {
            throw new IllegalArgumentException("Unsupported option: " + option);
        }

        return _getFrameAtTime(timeUs, option);
    }

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame close to the given time position if possible,
     * and returns it as a bitmap. This is useful for generating a thumbnail
     * for an input data source. Call this method if one does not care
     * how the frame is found as long as it is close to the given time;
     * otherwise, please call {@link #getFrameAtTime(long, int)}.
     *
     * @param timeUs The time position where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarentee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @return A Bitmap containing a representative video frame, which
     *         can be null, if such a frame cannot be retrieved.
     *
     * @see #getFrameAtTime(long, int)
     */
    public Bitmap getFrameAtTime(long timeUs) {
        return getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);
    }

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame at any time position if possible,
     * and returns it as a bitmap. This is useful for generating a thumbnail
     * for an input data source. Call this method if one does not
     * care about where the frame is located; otherwise, please call
     * {@link #getFrameAtTime(long)} or {@link #getFrameAtTime(long, int)}
     *
     * @return A Bitmap containing a representative video frame, which
     *         can be null, if such a frame cannot be retrieved.
     *
     * @see #getFrameAtTime(long)
     * @see #getFrameAtTime(long, int)
     */
    public Bitmap getFrameAtTime() {
        return getFrameAtTime(-1, OPTION_CLOSEST_SYNC);
    }

    private native Bitmap _getFrameAtTime(long timeUs, int option);
    /**
     * Option used in method {@link #getFrameAtTime(long, int)} to get a
     * frame at a specified location.
     *
     * @see #getFrameAtTime(long, int)
     */
    /* Do not change these option values without updating their counterparts
     * in include/media/stagefright/MediaSource.h!
     */
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * right before or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_PREVIOUS_SYNC    = 0x00;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * right after or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_NEXT_SYNC        = 0x01;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * closest to (in time) or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_CLOSEST_SYNC     = 0x02;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a frame (not necessarily a key frame) associated with a data source that
     * is located closest to or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_CLOSEST          = 0x03;


    /**
     * Call this method before setDataSource() so that the mode becomes
     * effective for subsequent operations. This method can be called only once
     * at the beginning if the intended mode of operation for a
     * MediaMetadataRetriever object remains the same for its whole lifetime,
     * and thus it is unnecessary to call this method each time setDataSource()
     * is called. If this is not never called (which is allowed), by default the
     * intended mode of operation is to both capture frame and retrieve meta
     * data (i.e., MODE_GET_METADATA_ONLY | MODE_CAPTURE_FRAME_ONLY).
     * Often, this may not be what one wants, since doing this has negative
     * performance impact on execution time of a call to setDataSource(), since
     * both types of operations may be time consuming.
     * 
     * @param mode The intended mode of operation. Can be any combination of 
     * MODE_GET_METADATA_ONLY and MODE_CAPTURE_FRAME_ONLY:
     * 1. MODE_GET_METADATA_ONLY & MODE_CAPTURE_FRAME_ONLY: 
     *    For neither frame capture nor meta data retrieval
     * 2. MODE_GET_METADATA_ONLY: For meta data retrieval only
     * 3. MODE_CAPTURE_FRAME_ONLY: For frame capture only
     * 4. MODE_GET_METADATA_ONLY | MODE_CAPTURE_FRAME_ONLY: 
     *    For both frame capture and meta data retrieval
     */
    public native void setMode(int mode);
    public native void setTime(int timeUs);

    /**
     * Sets the data source (file pathname) to use. Call this
     * method before the rest of the methods in this class. This method may be
     * time-consuming.
     * 
     * @param path The path of the input media file.
     * @throws IllegalArgumentException If the path is invalid.
     */
    public native void setDataSource(String path) throws IllegalArgumentException;
    
    /**
     * Sets the data source (FileDescriptor) to use.  It is the caller's
     * responsibility to close the file descriptor. It is safe to do so as soon
     * as this call returns. Call this method before the rest of the methods in
     * this class. This method may be time-consuming.
     * 
     * @param fd the FileDescriptor for the file you want to play
     * @param offset the offset into the file where the data to be played starts,
     * in bytes. It must be non-negative
     * @param length the length in bytes of the data to be played. It must be
     * non-negative.
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public native void setDataSource(FileDescriptor fd, long offset, long length)
            throws IllegalArgumentException;
    
    /**
     * Sets the data source (FileDescriptor) to use. It is the caller's
     * responsibility to close the file descriptor. It is safe to do so as soon
     * as this call returns. Call this method before the rest of the methods in
     * this class. This method may be time-consuming.
     * 
     * @param fd the FileDescriptor for the file you want to play
     * @throws IllegalArgumentException if the FileDescriptor is invalid
     */
    public void setDataSource(FileDescriptor fd)
            throws IllegalArgumentException {
        // intentionally less than LONG_MAX
        setDataSource(fd, 0, 0x7ffffffffffffffL);
    }
    
    /**
     * Call this method after setDataSource(). This method retrieves the 
     * meta data value associated with the keyCode.
     * 
     * The keyCode currently supported is listed below as METADATA_XXX
     * constants. With any other value, it returns a null pointer.
     * 
     * @param keyCode One of the constants listed below at the end of the class.
     * @return The meta data value associate with the given keyCode on success; 
     * null on failure.
     */
    public native String extractMetadata(int keyCode);

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame if successful and returns it as a bitmap. This is
     * useful for generating a thumbnail for an input media source.
     * 
     * @return A Bitmap containing a representative video frame, which 
     *         can be null, if such a frame cannot be retrieved.
     */
    public native Bitmap captureFrame();
    
    /**
     * Call this method after setDataSource(). This method finds the optional
     * graphic or album art associated (embedded or external url linked) the 
     * related data source.
     * 
     * @return null if no such graphic is found.
     */
    public native byte[] extractAlbumArt();

    /**
     * Call it when one is done with the object. This method releases the memory
     * allocated internally.
     */
    public native void release();
    private native void native_setup(); 

    private native final void native_finalize();

    @Override
    protected void finalize() throws Throwable {
        try {
            native_finalize();
        } finally {
            super.finalize();
        }
    }

    public static final int MODE_GET_METADATA_ONLY  = 0x01;
    public static final int MODE_CAPTURE_FRAME_ONLY = 0x02;

    /*
     * Do not change these values without updating their counterparts
     * in include/media/mediametadataretriever.h!
     */
    public static final int METADATA_KEY_CD_TRACK_NUMBER = 0;
    public static final int METADATA_KEY_ALBUM           = 1;
    public static final int METADATA_KEY_ARTIST          = 2;
    public static final int METADATA_KEY_AUTHOR          = 3;
    public static final int METADATA_KEY_COMPOSER        = 4;
    public static final int METADATA_KEY_DATE            = 5;
    public static final int METADATA_KEY_GENRE           = 6;
    public static final int METADATA_KEY_TITLE           = 7;
    public static final int METADATA_KEY_YEAR            = 8;
    public static final int METADATA_KEY_DURATION        = 9;
    public static final int METADATA_KEY_NUM_TRACKS     = 10;
}
