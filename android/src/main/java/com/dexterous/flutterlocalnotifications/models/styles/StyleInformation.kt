package com.dexterous.flutterlocalnotifications.models.styles

import android.graphics.Bitmap
import com.dexterous.flutterlocalnotifications.BitmapSource
import java.util.ArrayList

abstract class StyleInformation

open class DefaultStyleInformation(var contentTitle: String, var summaryText: String) : StyleInformation()

class BigTextStyleInformation(contentTitle: String, summaryText: String, var bigText: String) : DefaultStyleInformation(contentTitle, summaryText)


class BigPictureStyleInformation(contentTitle: String, summaryText: String, var largeIcon: String? = null, var largeIconBitmapSource: BitmapSource? = null, var bigPicture: String? = null, var bigPictureBitmapSource: BitmapSource? = null, var imageSource: BitmapSource? = null, var image: Bitmap? = null) : DefaultStyleInformation(contentTitle, summaryText)

class InboxStyleInformation(contentTitle: String, summaryText: String, var lines: ArrayList<String>) : DefaultStyleInformation(contentTitle, summaryText)

class MessageStyleInformation(var title: String, var message: String?, var timeStamp: Long? = null, var sender: String? = null, var isDirect:Boolean? = false) : StyleInformation()