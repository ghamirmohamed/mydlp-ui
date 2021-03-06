package com.mydlp.ui.widget.objects
{
	import mx.collections.ListCollectionView;

	[Bindable]
	public class ObjectGroup
	{
		
		protected var _label:String;
		protected var _children:ListCollectionView;
		protected var _classType:Class;
		
		public function set label(value:String): void
		{
			_label = value;
		}
		
		public function get label(): String
		{
			return _label;
		}
		
		public function set children(value:ListCollectionView): void
		{
			_children = value;
		}
		
		public function get children(): ListCollectionView
		{
			return _children;
		}
		
		public function set classType(value:Class): void
		{
			_classType = value;
		}
		
		public function get classType(): Class
		{
			return _classType;
		}
		
	}
}