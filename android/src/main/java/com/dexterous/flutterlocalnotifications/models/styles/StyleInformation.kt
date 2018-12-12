package com.dexterous.flutterlocalnotifications.models.styles

import com.dexterous.flutterlocalnotifications.BitmapSource
import java.util.ArrayList

abstract class StyleInformation

open class DefaultStyleInformation(var htmlFormatTitle: Boolean?, var htmlFormatBody: Boolean?) : StyleInformation()

class BigTextStyleInformation(htmlFormatTitle: Boolean?, htmlFormatBody: Boolean?, var bigText: String, var htmlFormatBigText: Boolean?, var contentTitle: String, var htmlFormatContentTitle: Boolean?, var summaryText: String, var htmlFormatSummaryText: Boolean?) : DefaultStyleInformation(htmlFormatTitle, htmlFormatBody)


class BigPictureStyleInformation(htmlFormatTitle: Boolean?, htmlFormatBody: Boolean?, var contentTitle: String, var htmlFormatContentTitle: Boolean?, var summaryText: String, var htmlFormatSummaryText: Boolean?, var largeIcon: String, var largeIconBitmapSource: BitmapSource, var bigPicture: String, var bigPictureBitmapSource: BitmapSource) : DefaultStyleInformation(htmlFormatTitle, htmlFormatBody)

class InboxStyleInformation(htmlFormatTitle: Boolean?, htmlFormatBody: Boolean?, var contentTitle: String, var htmlFormatContentTitle: Boolean?, var summaryText: String, var htmlFormatSummaryText: Boolean?, var lines: ArrayList<String>, var htmlFormatLines: Boolean?) : DefaultStyleInformation(htmlFormatTitle, htmlFormatBody)