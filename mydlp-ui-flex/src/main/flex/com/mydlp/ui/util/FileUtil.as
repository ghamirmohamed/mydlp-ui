package com.mydlp.ui.util
{	
	public class FileUtil
	{
		public static function getHumanReadableSize(size:Number): String
		{			
			if(size < 1024)
				return size + " B";
			
			size /= 1024;
			if(size < 1024)
				return size.toFixed(2) + " KB";
			
			size /= 1024;
			if(size < 1024)
				return size.toFixed(2) + " MB";
			
			size /= 1024;
			if(size < 1024)
				return size.toFixed(2) + " GB";
			
			else
				return size.toFixed(2) + " GB";

		}
	}
}