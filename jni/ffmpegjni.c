/*
 * Copyright (C) 2009 The Android Open Source Project
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
 *
 */
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include "libavcodec/avcodec.h"
#include "libavcodec/dirac.h"
#include "libavcodec/opt.h"
#include "libavformat/avformat.h"
#include "libavformat/avio.h"
#include "libavutil/pixdesc.h"
#include "libswscale/swscale.h"


#define  LOG_TAG    "libffmpeg"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static AVFormatContext *fmt_ctx = NULL;
AVCodecContext *avctxVideo = NULL;

static int video_stream_id = -1;
jint 
Java_org_hansdeveloper_stream2android_ffmpegjni_Init( JNIEnv* env,
                                                  jobject thiz)
{
	URLProtocol * up = NULL;
	int numprotocols = 0;

	av_register_all();
	
	up = first_protocol;
	if (up == NULL)
        return (jint)-1;
	
	while (up != NULL) {
		numprotocols++;
		LOGI("Init: protocol %s\n", up->name);
        if (!strcmp("tcp", up->name))
            return (jint)numprotocols;
        up = up->next;
    }
	
    return (jint)avcodec_version();
}
/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 */
jstring
Java_org_hansdeveloper_stream2android_ffmpegjni_Open( JNIEnv* env, jobject thiz, 
												  jstring path )
{
	const char *nativeString = (*env)->GetStringUTFChars(env, path, 0);
    int err, i, ret;
	char pErrorMsg[1000];
	int stream_idx;
	
	err = av_open_input_file(&fmt_ctx, nativeString, NULL, 0, NULL);
	if( err < 0 )
	{
		snprintf(pErrorMsg, 1000, "FAILED to open file! err=%d", err);
		(*env)->ReleaseStringUTFChars(env, path, nativeString);
		return (*env)->NewStringUTF(env, pErrorMsg);
	}
    err = av_find_stream_info(fmt_ctx);
	if( err < 0 )
	{
		snprintf(pErrorMsg, 1000, "FAILED to get streaminfo! err=%d", err);
		if (fmt_ctx)
		{
			av_close_input_file(fmt_ctx);
			fmt_ctx = NULL;
		}
		(*env)->ReleaseStringUTFChars(env, path, nativeString);
		return (*env)->NewStringUTF(env, pErrorMsg);
	}
	

	(*env)->ReleaseStringUTFChars(env, path, nativeString);
	
	for ( stream_idx = 0; stream_idx < fmt_ctx->nb_streams; stream_idx++)
	{
		AVCodecContext *avctx = fmt_ctx->streams[stream_idx]->codec;
		if (avctx && avctx->codec_type == AVMEDIA_TYPE_VIDEO)
		{
			video_stream_id = stream_idx;
		}
	}	
	
	return (*env)->NewStringUTF(env, "SUCCEEDED to open file!");
}
jint 
Java_org_hansdeveloper_stream2android_ffmpegjni_GetStreamCount( JNIEnv* env, jobject thiz)
{
	int numstreams = 0; 
	if(fmt_ctx)
	{
		numstreams = fmt_ctx->nb_streams;
	}
	return numstreams;
}

jint 
Java_org_hansdeveloper_stream2android_ffmpegjni_GetStreamCodecType( JNIEnv* env, jobject thiz, 
												  int streamnum)
{
	int codectype = 0; 
	if (streamnum >= fmt_ctx->nb_streams)
		return -1;
	if(fmt_ctx)
	{
		if (fmt_ctx->streams[streamnum]->codec)
			codectype = fmt_ctx->streams[streamnum]->codec->codec_type;
	}
	return codectype;
}
static void pgm_save(unsigned char *buf, int wrap, int xsize, int ysize,
                     const char *filename)
{
    FILE *f;
    int i;
	LOGI("pgm_save: wrap=%d, xsize=%d, ysize=%d, '%s'\n", wrap, xsize, ysize, filename);
    f=fopen(filename,"w");
    fprintf(f,"P5\n%d %d\n%d\n",xsize,ysize,255);
    for(i=0;i<ysize;i++)
        fwrite(buf + i * wrap,1,xsize,f);
    fclose(f);
}
static void bmp_save(unsigned char *buf, int wrap, int xsize, int ysize,
                     const char *filename)
{
    FILE *f;
    int i;
	LOGI("bmp_save: wrap=%d, xsize=%d, ysize=%d, '%s'\n", wrap, xsize, ysize, filename);
    f = fopen(filename,"wb");
    
	fwrite("BM", 1, 2, f);
	// size of file 4 bytes
	i = xsize * ysize * 3 + 14 + 40;
	fwrite(&i, 1, 4, f);
	// 4 bytes reserved
	i = 0;
	fwrite(&i, 1, 4, f);
	// 4 bytes offset to pixels
	i = 14 + 40;
	fwrite(&i, 1, 4, f);
	// bitmapinfoheader size
	i = 40;
	fwrite(&i, 1, 4, f);
// width
	fwrite(&xsize, 1, 4, f);
//height	
	fwrite(&ysize, 1, 4, f);
	// planes
	i = 1;
	fwrite(&i, 1, 2, f);
	// bpp
	i = 24;
	fwrite(&i, 1, 2, f);
	// compression
	i = 0;
	fwrite(&i, 1, 4, f);
	// size of image 4 bytes
	i = xsize * ysize * 3;
	fwrite(&i, 1, 4, f);
	i = 0;
	fwrite(&i, 1, 4, f);
	i = 0;
	fwrite(&i, 1, 4, f);
	i = 0;
	fwrite(&i, 1, 4, f);
	i = 0;
	fwrite(&i, 1, 4, f);

    for(i=0;i<ysize;i++)
        fwrite(buf + (ysize - i - 1) * wrap, 1, xsize * 3, f);
    
	fclose(f);
	LOGI("bmp_save: leave\n");
}
static AVFrame *alloc_picture(enum PixelFormat pix_fmt, int width, int height)
{
    AVFrame *picture;
    uint8_t *picture_buf;
    int size;

    picture = avcodec_alloc_frame();
    if (!picture)
        return NULL;
    size = avpicture_get_size(pix_fmt, width, height);
    picture_buf = av_malloc(size);
    if (!picture_buf) {
        av_free(picture);
        return NULL;
    }
    avpicture_fill((AVPicture *)picture, picture_buf,
                   pix_fmt, width, height);
    return picture;
}
jint
Java_org_hansdeveloper_stream2android_ffmpegjni_GetNextPicture(JNIEnv* env,
                                                  jobject thiz, 
												  jobject bitmap)
{
    AVPacket pkt1, *pkt = &pkt1;
    struct SwsContext *img_convert_ctx;
	static int sws_flags = SWS_BICUBIC;
	AndroidBitmapInfo  info;
	void*              pixels;
	int codecid = 0;
	int ret = 0;

	if(fmt_ctx && avctxVideo)
	{
		if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
			LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
			return;
		}

		if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
			LOGE("Bitmap format is not RGB_565 !");
			return;
		}

		if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
			LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		}

		while (1)
		{
			int got_picture;

			av_init_packet(pkt);
			pkt->data = NULL;
			pkt->size = 0;
			pkt->stream_index = -1;
			
			if (av_read_frame(fmt_ctx, pkt) < 0) 
			{
				// error reading frame
				av_free_packet(pkt);
				codecid = -5;
				break;
			}
			else
			{
				if (pkt->stream_index != video_stream_id)
				{
					av_free_packet(pkt);
					continue;
				}
				else
				{
					AVFrame *frame = avcodec_alloc_frame();
					frame->data[0] = 0;
					frame->data[1] = 0;
					frame->data[2] = 0;
					frame->data[3] = 0;

					got_picture = 0;
					avcodec_decode_video2(avctxVideo, frame, &got_picture, pkt);
					if (got_picture)
					{
						img_convert_ctx = sws_getContext(avctxVideo->width, avctxVideo->height,
														 avctxVideo->pix_fmt,
														 avctxVideo->width, 
														 avctxVideo->height,
														 PIX_FMT_RGB565LE,
														 sws_flags, NULL, NULL, NULL);
						if (img_convert_ctx == NULL) 
						{
							codecid = -6;
						}
						else
						{
							AVFrame *tmp_picture;
							tmp_picture->data[0] = 0;
							tmp_picture = alloc_picture(PIX_FMT_RGB565LE, avctxVideo->width, avctxVideo->height);
							
							avpicture_fill((AVPicture *)tmp_picture, pixels,
										   PIX_FMT_RGB565LE, 
										   info.width, 
										   info.height);
							if(tmp_picture)
							{
								LOGI("scale to temp rgb32 picture: linesize=%d\n", tmp_picture->linesize[0]);
								sws_scale(img_convert_ctx, 
									frame->data, 
									frame->linesize,
									0, avctxVideo->height, 
									tmp_picture->data, 
									tmp_picture->linesize);
								
								av_free(tmp_picture);
							}
							else
							{
								codecid = -7;
							}
						}
						av_free(frame);
						av_free_packet(pkt);
					}
					else
					{
						av_free(frame);
						av_free_packet(pkt);
						codecid -= 1;
						if (codecid > -10000)
							continue;
					}
					break;
				}
			}
		} // while (1)

		AndroidBitmap_unlockPixels(env, bitmap);
	}
	return codecid;
}
jint 
Java_org_hansdeveloper_stream2android_ffmpegjni_GetFirstPicture( JNIEnv* env,
                                                  jobject thiz, 
												  int streamnum,
												  jstring path)
{
	return 0;
}
jint 
Java_org_hansdeveloper_stream2android_ffmpegjni_GetStreamCodecID( JNIEnv* env,
                                                  jobject thiz, 
												  int streamnum)
{
	AVCodecContext *avctx = NULL;
	int codecid = 0; 
	if(fmt_ctx)
	{
		if (streamnum >= fmt_ctx->nb_streams)
			return -1;
		if (fmt_ctx->streams[streamnum] &&
			fmt_ctx->streams[streamnum]->codec)
		{
			avctx = fmt_ctx->streams[streamnum]->codec;
			
			codecid = avctx->codec_id;	
		}
	}
	return codecid;
}

jint 
Java_org_hansdeveloper_stream2android_ffmpegjni_GetFramePictureAt( JNIEnv* env,
                                                  jobject thiz, 
												  int streamnum,
												  jstring path,
												  int seconds)
{
	AVCodecContext *avctx = NULL;
    AVCodec *codec = NULL;
    AVPacket pkt1, *pkt = &pkt1;
    struct SwsContext *img_convert_ctx;
	static int sws_flags = SWS_BICUBIC;
	void*              pixels;
	
	int codecid = 0; 
	if(fmt_ctx)
	{
		if (streamnum >= fmt_ctx->nb_streams)
			return -1;
		if (fmt_ctx->streams[streamnum] &&
			fmt_ctx->streams[streamnum]->codec)
		{
			avctx = fmt_ctx->streams[streamnum]->codec;
			
			codecid = avctx->codec_id;
			codec = avcodec_find_decoder(avctx->codec_id);
			if (!codec)
				return -2;
				
			if (avcodec_thread_init(avctx, 1) < 0)
			{
				return -3;
			}
			
			if(avcodec_open(avctx, codec) < 0)
			{
				return -4;
			}
	
			if (avctx->codec_type == AVMEDIA_TYPE_VIDEO) 
			{	
				int capturenow = 0;
				int framenum  = 0;
				avctxVideo = avctx;
				codecid = -10;
				while (1)
				{
					int got_picture;
					
					av_init_packet(pkt);
					pkt->data = NULL;
					pkt->size = 0;
					pkt->stream_index = -1;
					pkt->pts = 0LL;
					pkt->dts = 0LL;
					
					if (av_read_frame(fmt_ctx, pkt) < 0) 
					{
						// error reading frame
						av_free_packet(pkt);
						LOGE("GetFramePictureAt: error reading frame\n");
						codecid = -5;
						break;
					}
					else
					{
						if (pkt->stream_index != streamnum)
						{
							av_free_packet(pkt);
							continue;
						}
						else
						{
							double pts = 0.0f;
							if(pkt->dts != AV_NOPTS_VALUE) {
								pts = pkt->dts;
							} else {
								if(pkt->pts != AV_NOPTS_VALUE) {
									pts = pkt->pts;
								} else {
									capturenow = 1;
								}
							}
							pts *= av_q2d(fmt_ctx->streams[streamnum]->time_base);
							LOGI("GetFramePictureAt: packet pts from videostream %f, flags=%d\n", pts, pkt->flags);
							//LOGI("GetStreamCodecID: packet pts from videostream %lu\n", pkt->pts);
							//LOGI("GetStreamCodecID: packet dts from videostream %lu\n", pkt->dts);
							
							if (pts >= seconds 
								&& pkt->flags == AV_PKT_FLAG_KEY)
								capturenow = 1;
							
							if (capturenow)
							{
								AVFrame *frame = avcodec_alloc_frame();

								got_picture = 0;
								avcodec_decode_video2(avctxVideo, frame, &got_picture, pkt);
								LOGI("GetFramePictureAt: decoded a frame got_picture=%d\n", got_picture);
								if (got_picture)
								{
									framenum++;
									const char *nativeString = (*env)->GetStringUTFChars(env, path, 0);
									//pgm_save(frame->data[0], frame->linesize[0], avctx->width, avctx->height, nativeString);
									
									//if (AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
									//}
									//if (AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
									//}
									img_convert_ctx = sws_getContext(
										avctxVideo->width, 
										avctxVideo->height,
										avctxVideo->pix_fmt,
										avctxVideo->width, 
										avctxVideo->height,
										PIX_FMT_BGR24,
										sws_flags, NULL, NULL, NULL);
									if (img_convert_ctx == NULL) 
									{
										LOGE("GetFramePictureAt: a swsContext could be allocated\n");
										codecid = -6;
									}
									else
									{
										AVFrame *tmp_picture;
										//tmp_picture->data[0] = 0;
										tmp_picture = alloc_picture(PIX_FMT_BGR24, avctxVideo->width, avctxVideo->height);
										
										if(tmp_picture)
										{
											LOGI("GetFramePictureAt: scale to temp rgb24 picture: linesize=%d, pts=%f\n", tmp_picture->linesize[0], pts);
											sws_scale(
												img_convert_ctx, 
												frame->data, 
												frame->linesize,
												0, avctxVideo->height, 
												tmp_picture->data, 
												tmp_picture->linesize);
												
											bmp_save(
												tmp_picture->data[0], 
												tmp_picture->linesize[0], 
												avctxVideo->width, 
												avctxVideo->height, 
												nativeString);
									
											av_free(tmp_picture);
										}
										else
										{
											LOGE("GetFramePictureAt: an AVFrame could not be allocated\n");
											codecid = -7;
										}
									}
									//AndroidBitmap_unlockPixels(env, bitmap);
									(*env)->ReleaseStringUTFChars(env, path, nativeString);
									av_free(frame);
									av_free_packet(pkt);
									// we have a picture, quit
									break;
								}
								else
								{
									// no picture, discard the frame and the packet
									av_free(frame);
									av_free_packet(pkt);
									codecid -= 1;
									if (codecid > -10000)
										continue;
									else
										// if no picture after 10000 packets,  give up
										break;
								}
							}
							else
							{
								codecid -= 1;
								// if no packet within 10 sec, after 10000 packets, give up
								if (codecid < -10000)
									break;
							}
						}
					}
				} // while (1)
			}
		}
	}
	LOGI("GetFramePictureAt: return %d to java\n", codecid);
	return codecid;
}
static const char *binary_unit_prefixes [] = { "", "Ki", "Mi", "Gi", "Ti", "Pi" };
static const char *decimal_unit_prefixes[] = { "", "K" , "M" , "G" , "T" , "P"  };

static const char *unit_second_str          = "s"    ;
static const char *unit_hertz_str           = "Hz"   ;
static const char *unit_byte_str            = "byte" ;
static const char *unit_bit_per_second_str  = "bit/s";

static int convert_tags                 = 0;
static int show_value_unit              = 0;
static int use_value_prefix             = 0;
static int use_byte_value_binary_prefix = 0;
static int use_value_sexagesimal_format = 0;
static const char *media_type_string(enum AVMediaType media_type)
{
    switch (media_type) {
    case AVMEDIA_TYPE_VIDEO:      return "video";
    case AVMEDIA_TYPE_AUDIO:      return "audio";
    case AVMEDIA_TYPE_DATA:       return "data";
    case AVMEDIA_TYPE_SUBTITLE:   return "subtitle";
    case AVMEDIA_TYPE_ATTACHMENT: return "attachment";
    default:                      return "unknown";
    }
}


static char *value_string(char *buf, int buf_size, double val, const char *unit)
{
    if (unit == unit_second_str 
	//&& use_value_sexagesimal_format
	) {
        double secs;
        int hours, mins;
        secs  = val;
        mins  = (int)secs / 60;
        secs  = secs - mins * 60;
        hours = mins / 60;
        mins %= 60;
        snprintf(buf, buf_size, "%d:%02d:%09.6f", hours, mins, secs);
    } else if (use_value_prefix) {
        const char *prefix_string;
        int index;

        if (unit == unit_byte_str && use_byte_value_binary_prefix) {
            index = (int) (log(val)/log(2)) / 10;
            index = av_clip(index, 0, FF_ARRAY_ELEMS(binary_unit_prefixes) -1);
            val /= pow(2, index*10);
            prefix_string = binary_unit_prefixes[index];
        } else {
            index = (int) (log10(val)) / 3;
            index = av_clip(index, 0, FF_ARRAY_ELEMS(decimal_unit_prefixes) -1);
            val /= pow(10, index*3);
            prefix_string = decimal_unit_prefixes[index];
        }

        snprintf(buf, buf_size, "%.3f %s%s", val, prefix_string, show_value_unit ? unit : "");
    } else {
        snprintf(buf, buf_size, "%f %s", val, show_value_unit ? unit : "");
    }

    return buf;
}
static char *time_value_string(char *buf, int buf_size, int64_t val, const AVRational *time_base)
{
    if (val == AV_NOPTS_VALUE) {
        snprintf(buf, buf_size, "N/A");
    } else {
        value_string(buf, buf_size, val * av_q2d(*time_base), unit_second_str);
    }

    return buf;
}
jstring 
Java_org_hansdeveloper_stream2android_ffmpegjni_showformat( JNIEnv* env,
                                                  jobject thiz)
{
    AVMetadataTag *tag = NULL;
    char val_str[128];
    char line_str[512];
    char ret_str[4096];

	if (!fmt_ctx)
	{
		return (*env)->NewStringUTF(env, "AVStreamFormat not set");
	}
		
    strcpy(ret_str, "[FORMAT]\n");

    snprintf(line_str, 512, "\tfilename=%s\n",         fmt_ctx->filename);
	av_strlcat(ret_str, line_str, 4096);

    snprintf(line_str, 512, "\tnb_streams=%d\n",       fmt_ctx->nb_streams);
	av_strlcat(ret_str, line_str, 4096);

    snprintf(line_str, 512, "\tformat_name=%s\n",      fmt_ctx->iformat->name);
	av_strlcat(ret_str, line_str, 4096);

    snprintf(line_str, 512, "\tformat_long_name=%s\n", fmt_ctx->iformat->long_name);
	av_strlcat(ret_str, line_str, 4096);

    snprintf(line_str, 512, "\tstart_time=%s\n",       time_value_string(val_str, sizeof(val_str), fmt_ctx->start_time,
                                                      &AV_TIME_BASE_Q));
	av_strlcat(ret_str, line_str, 4096);

    snprintf(line_str, 512, "\tduration=%s\n",         time_value_string(val_str, sizeof(val_str), fmt_ctx->duration,
                                                      &AV_TIME_BASE_Q));
	av_strlcat(ret_str, line_str, 4096);

    snprintf(line_str, 512, "\tsize=%s\n",             value_string(val_str, sizeof(val_str), fmt_ctx->file_size,
                                                 unit_byte_str));
	av_strlcat(ret_str, line_str, 4096);

    snprintf(line_str, 512, "\tbit_rate=%s\n",         value_string(val_str, sizeof(val_str), fmt_ctx->bit_rate,
                                                 unit_bit_per_second_str));
	av_strlcat(ret_str, line_str, 4096);

//    if (convert_tags)
//        av_metadata_conv(fmt_ctx, NULL, fmt_ctx->iformat->metadata_conv);
//    while ((tag = av_metadata_get(fmt_ctx->metadata, "", tag, AV_METADATA_IGNORE_SUFFIX)))
//        snprintf("\tTAG:%s=%s\n", tag->key, tag->value);

    snprintf(line_str, 512, "[/FORMAT]\n");
	av_strlcat(ret_str, line_str, 4096);
	return (*env)->NewStringUTF(env, ret_str);
}

jstring 
Java_org_hansdeveloper_stream2android_ffmpegjni_showstreaminfo( JNIEnv* env,
                                                  jobject thiz, int stream_idx)
{
    AVStream *stream = fmt_ctx->streams[stream_idx];
    AVCodecContext *dec_ctx;
    AVCodec *dec;
    AVMetadataTag *tag = NULL;
    char a, b, c, d;
    AVRational display_aspect_ratio;

    char val_str[128];
    char line_str[512];
    char ret_str[4096];

	if (!fmt_ctx)
	{
		return (*env)->NewStringUTF(env, "AVStreamFormat not set");
	}
	if (stream_idx >= fmt_ctx->nb_streams)
		return (*env)->NewStringUTF(env, "stream_id invalid");

    strcpy(ret_str, "[STREAM]\n");

    snprintf(line_str, 512, "index=%d\n",        stream->index);
	av_strlcat(ret_str, line_str, 4096);

    if ((dec_ctx = stream->codec)) {
        if ((dec = dec_ctx->codec)) {
            snprintf(line_str, 512, "\tcodec_name=%s\n",         dec->name);
			av_strlcat(ret_str, line_str, 4096);
            snprintf(line_str, 512, "\tcodec_long_name=%s\n",    dec->long_name);
			av_strlcat(ret_str, line_str, 4096);
        } else {
            snprintf(line_str, 512, "\tcodec_name=unknown\n");
			av_strlcat(ret_str, line_str, 4096);
        }

        snprintf(line_str, 512, "\tcodec_type=%s\n",         media_type_string(dec_ctx->codec_type));
		av_strlcat(ret_str, line_str, 4096);
        snprintf(line_str, 512, "\tcodec_time_base=%d/%d\n", dec_ctx->time_base.num, dec_ctx->time_base.den);
		av_strlcat(ret_str, line_str, 4096);

        // print AVI/FourCC tag 
        a = dec_ctx->codec_tag     & 0xff;
        b = dec_ctx->codec_tag>>8  & 0xff;
        c = dec_ctx->codec_tag>>16 & 0xff;
        d = dec_ctx->codec_tag>>24 & 0xff;
        snprintf(line_str, 512, "\tcodec_tag_string=");
		av_strlcat(ret_str, line_str, 4096);
        if (isprint(a)) snprintf(line_str, 512, "%c", a); else snprintf(line_str, 512, "[%d]", a);
		av_strlcat(ret_str, line_str, 4096);
        if (isprint(b)) snprintf(line_str, 512, "%c", b); else snprintf(line_str, 512, "[%d]", b);
		av_strlcat(ret_str, line_str, 4096);
        if (isprint(c)) snprintf(line_str, 512, "%c", c); else snprintf(line_str, 512, "[%d]", c);
		av_strlcat(ret_str, line_str, 4096);
        if (isprint(d)) snprintf(line_str, 512, "%c", d); else snprintf(line_str, 512, "[%d]", d);
		av_strlcat(ret_str, line_str, 4096);
        snprintf(line_str, 512, "\n\tcodec_tag=0x%04x\n", dec_ctx->codec_tag);
		av_strlcat(ret_str, line_str, 4096);

        switch (dec_ctx->codec_type) {
        case AVMEDIA_TYPE_VIDEO:
            snprintf(line_str, 512, "\twidth=%d\n",                   dec_ctx->width);
			av_strlcat(ret_str, line_str, 4096);
            snprintf(line_str, 512, "\theight=%d\n",                  dec_ctx->height);
			av_strlcat(ret_str, line_str, 4096);
            snprintf(line_str, 512, "\thas_b_frames=%d\n",            dec_ctx->has_b_frames);
			av_strlcat(ret_str, line_str, 4096);
            if (dec_ctx->sample_aspect_ratio.num) {
                snprintf(line_str, 512, "\tsample_aspect_ratio=%d:%d\n", dec_ctx->sample_aspect_ratio.num,
                                                      dec_ctx->sample_aspect_ratio.den);
				av_strlcat(ret_str, line_str, 4096);
                av_reduce(&display_aspect_ratio.num, &display_aspect_ratio.den,
                          dec_ctx->width  * dec_ctx->sample_aspect_ratio.num,
                          dec_ctx->height * dec_ctx->sample_aspect_ratio.den,
                          1024*1024);
                snprintf(line_str, 512, "\tdisplay_aspect_ratio=%d:%d\n", display_aspect_ratio.num,
                                                       display_aspect_ratio.den);
				av_strlcat(ret_str, line_str, 4096);
            }
            snprintf(line_str, 512, "\tpix_fmt=%s\n",                 dec_ctx->pix_fmt != PIX_FMT_NONE ?
                   av_pix_fmt_descriptors[dec_ctx->pix_fmt].name : "unknown");
			av_strlcat(ret_str, line_str, 4096);
            break;

        case AVMEDIA_TYPE_AUDIO:
            snprintf(line_str, 512, "\tsample_rate=%s\n",             value_string(val_str, sizeof(val_str),
                                                                dec_ctx->sample_rate,
                                                                unit_hertz_str));
			av_strlcat(ret_str, line_str, 4096);
            snprintf(line_str, 512, "\tchannels=%d\n",                dec_ctx->channels);
			av_strlcat(ret_str, line_str, 4096);
            snprintf(line_str, 512, "\tbits_per_sample=%d\n",         av_get_bits_per_sample(dec_ctx->codec_id));
			av_strlcat(ret_str, line_str, 4096);
            break;
        }
    } else {
        snprintf(line_str, 512, "\tcodec_type=unknown\n");
		av_strlcat(ret_str, line_str, 4096);
    }

    if (fmt_ctx->iformat->flags & AVFMT_SHOW_IDS)
    {
		snprintf(line_str, 512, "\tid=0x%x\n", stream->id);
		av_strlcat(ret_str, line_str, 4096);
    }
	snprintf(line_str, 512, "\tr_frame_rate=%d/%d\n",         stream->r_frame_rate.num,   stream->r_frame_rate.den);
	av_strlcat(ret_str, line_str, 4096);
    snprintf(line_str, 512, "\tavg_frame_rate=%d/%d\n",       stream->avg_frame_rate.num, stream->avg_frame_rate.den);
	av_strlcat(ret_str, line_str, 4096);
    snprintf(line_str, 512, "\ttime_base=%d/%d\n",            stream->time_base.num,      stream->time_base.den);
	av_strlcat(ret_str, line_str, 4096);
    if (stream->language[0])
	{
		snprintf(line_str, 512, "\tlanguage=%s\n",            stream->language);
		av_strlcat(ret_str, line_str, 4096);
    }
	snprintf(line_str, 512, "\tstart_time=%s\n",   time_value_string(val_str, sizeof(val_str), stream->start_time,
                                                  &stream->time_base));
	av_strlcat(ret_str, line_str, 4096);
    snprintf(line_str, 512, "\tduration=%s\n",     time_value_string(val_str, sizeof(val_str), stream->duration,
                                                  &stream->time_base));
	av_strlcat(ret_str, line_str, 4096);
    if (stream->nb_frames)
	{
        snprintf(line_str, 512, "\tnb_frames=%"PRId64"\n",    stream->nb_frames);
		av_strlcat(ret_str, line_str, 4096);
	}
    while ((tag = av_metadata_get(stream->metadata, "", tag, AV_METADATA_IGNORE_SUFFIX)))
    {
		snprintf(line_str, 512, "\tTAG:%s=%s\n", tag->key, tag->value);
		av_strlcat(ret_str, line_str, 4096);
	}
    snprintf(line_str, 512, "[/STREAM]\n");
	av_strlcat(ret_str, line_str, 4096);
	return (*env)->NewStringUTF(env, ret_str);
}
void 
Java_org_hansdeveloper_stream2android_ffmpegjni_Close( JNIEnv* env,
                                                  jobject thiz)
{
	if(fmt_ctx)
	{
		av_close_input_file(fmt_ctx);
	}
	avctxVideo = NULL;
	
}

void dumvoiv()
{
    avcodec_version();
	avcodec_register_all();
	ff_dirac_parse_sequence_header(0, 0, 0);
	av_opt_set_defaults(0);
	avcodec_alloc_context();
	av_register_all();
	//sws_rgb2rgb_init
	//ff_yuv2rgb_c_init_tables(NULL, NULL, 0, 0, 0, 0);
	sws_getContext(22, 22, PIX_FMT_YUV420P, 22, 22, PIX_FMT_YUV420P, 0, NULL, NULL, NULL);
//ff_network_init();
}