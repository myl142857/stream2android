/*

 */
package org.hansdeveloper.Stream2Android;

public class ffmpegjni
{
	public ffmpegjni()
	{
        Integer version = Init();
	}
/*			File file = new File(v.parentdirectory, ".frame.bmp");
			Open(f);
			v.description = showformat();

			int video_streamID = -1;
			Integer numStreams = GetStreamCount();
	        for (int c = 0; c < numStreams ; c++)
	        {
	        	Integer codec_type = GetStreamCodecType(c);
	        	String streamInfo = showstreaminfo(c);
	        	v.description += "\r\ncodec_type: " + codec_type + "\r\n" + streamInfo;

	        	if (codec_type == 0)
	        	{
	        		video_streamID = c;
	        		//v.resolution = "";
	        		//v.duration = ;
	        	}
	        }
			if (bm == null 
					&& video_streamID > -1)
			{
				Log.d("ffmpeg", "GetFramePictureAt steamindex=" + video_streamID + " file=" + file.getAbsolutePath());
				Integer codec_id = 0;
				
				if (file.exists())
				{
					// use cached bitmap image
					codec_id = 1;
				}
				else
				{
					// create bitmap image
					codec_id = GetFramePictureAt(video_streamID, file.getAbsolutePath(), 3);
				}
				// load bitmap to vi.largeimage
				if (codec_id > -10000 
						&& (codec_id > 0 || codec_id <= -10) 
						&& file.length() > 0)
				{
					try {
						// convert and resize bitmap
						String pathName = file.getAbsolutePath();
						Bitmap bm1 = BitmapFactory.decodeFile(pathName);
						if (bm1 != null)
						{
							boolean filter = false;
							java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
							Bitmap.createScaledBitmap(bm1, 800, 480, filter).compress(Bitmap.CompressFormat.JPEG, 80, os);
							v.largeimage = os.toByteArray();
							
							os = new java.io.ByteArrayOutputStream();
							// create MINI_KINd thumbnail
							bm = Bitmap.createScaledBitmap(bm1, 96, 96, filter);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			Close();
		}
        //
		
		if (bm != null)
		{
			java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
			bm.compress(Bitmap.CompressFormat.JPEG, 80, os);
//			v.image = os.toByteArray();
		}
	}
*/
    /* A native method that is implemented by the
     * 'ffmpegjni' native library, which is packaged
     * with this application.
     */
    public native String Open(String path);

    /* This is another native method declaration that is *not*
     * implemented by 'ffmpegjni'. This is simply to show that
     * you can declare as many native methods in your Java code
     * as you want, their implementation is searched in the
     * currently loaded native libraries only the first time
     * you call them.
     *
     * Trying to call this function will result in a
     * java.lang.UnsatisfiedLinkError exception !
     */
    public native int Init();
    public native int GetStreamCount();
    public native int GetStreamCodecType(int stream_idx);
    public native int GetStreamCodecID(int stream_idx);
	public native int GetFramePictureAt(int stream_idx, String path, int seconds);
    public native String showformat();
    public native String showstreaminfo(int stream_idx);
    public native int Close();
    /* this is used to load the 'ffmpegjni' library on application
     * startup. The library has already been unpacked into
     * ??/lib/ffmpegjni.so at
     * installation time by the package manager.
     */
    static {
        System.loadLibrary("ffmpegjni");
    }
}
