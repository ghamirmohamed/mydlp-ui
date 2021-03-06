package com.mydlp.ui.util
{
	import avmplus.getQualifiedClassName;
	
	import com.mydlp.ui.domain.AbstractEntity;
	import com.mydlp.ui.domain.DocumentDatabase;
	import com.mydlp.ui.domain.InformationFeature;
	import com.mydlp.ui.domain.InformationType;
	import com.mydlp.ui.domain.InventoryCategory;
	import com.mydlp.ui.domain.InventoryItem;
	import com.mydlp.ui.domain.Item;
	import com.mydlp.ui.domain.Matcher;
	import com.mydlp.ui.domain.MatcherArgument;
	import com.mydlp.ui.domain.RegularExpressionGroup;
	import com.mydlp.ui.domain.Rule;
	import com.mydlp.ui.domain.RuleItem;
	import com.mydlp.ui.widget.policy.inventory.InventoryItemRenderer;
	
	import flash.utils.describeType;
	import flash.utils.getDefinitionByName;
	import flash.utils.getQualifiedClassName;
	
	import mx.collections.ArrayCollection;
	import mx.collections.ArrayList;
	import mx.collections.ListCollectionView;
	import mx.controls.Alert;
	import mx.core.FlexGlobals;

	public class ReflectionUtil
	{
		public static function getClassMembers(className:*): Array
		{
			var classMembers:ArrayList = new ArrayList();
			var description:XML = describeType(className);
			
			for each (var i:XML in description..accessor){
				var memberName:String = i.@name;
				if (
					memberName == "icon" ||
					memberName == "prototype" ||
					memberName == "proxyInitialized" ||
					memberName == "proxyKey" ||
					memberName == "stateManaged" ||
					memberName == "uid" ||
					memberName == "label"
					)
					continue;
				var classMember:ClassMember = new ClassMember();
				classMember.name = memberName;
				var memberType:String = i.@type;
				classMember.type = getDefinitionByName(memberType) as Class;
				classMembers.addItem(classMember);
			}
			return classMembers.toArray();
		}
		
		public static function hasMember(className:*, memberName:String): Boolean
		{
			var classMembers:Array = ReflectionUtil.getClassMembers(className);
			for each (var classMember:ClassMember in classMembers)
				if (classMember.name == memberName)
					return true;
			return false;
		}
		
		public static function hasMemberOfType(className:*, memberName:String, memberType:Class): Boolean
		{
			var classMembers:Array = ReflectionUtil.getClassMembers(className);
			for each (var classMember:ClassMember in classMembers)
			if (classMember.name == memberName && classMember.type == memberType)
				return true;
			return false;
		}
		
		public static function hasMemberType(className:*, memberType:Class): Boolean
		{
			var classMembers:Array = ReflectionUtil.getClassMembers(className);
			for each (var classMember:ClassMember in classMembers)
			if (classMember.type == memberType)
				return true;
			return false;
		}
		
		
		public static function cloneDomainObject(sourceObject:Object): *
		{
			var targetObject:*;
			
			if (sourceObject is AbstractEntity)
			{
				var targetClass:Class = getDefinitionByName(getQualifiedClassName(sourceObject)) as Class;
				targetObject = new targetClass();
				
				var classMembers:Array = ReflectionUtil.getClassMembers(sourceObject as Object);
				for each (var classMember:ClassMember in classMembers)
					if (classMember.name == "id" )
					{
						continue;
					}
					else if (
							(classMember.name == "children" && classMember.type == ListCollectionView) ||
							(classMember.name == "nameKey" && classMember.type == String) ||
							(classMember.name == "label" && classMember.type == String) ||
							(classMember.name == "coupledInventoryItem" && classMember.type == InventoryItem) ||
							(sourceObject is Matcher && classMember.name == "coupledInformationFeature" && classMember.type == InformationFeature)
						)
					{
						continue;
					}
					else if (classMember.name == "category" && classMember.type == InventoryCategory)
					{
						var c:InventoryCategory = sourceObject[classMember.name];
						if (InventoryItemRenderer.isAddEnabled(c))
							targetObject[classMember.name] = sourceObject[classMember.name];
						else
							targetObject[classMember.name] = FlexGlobals.topLevelApplication.inventoryTree.getUserDefinedCategory();
					}
					else if (classMember.name == "name" && classMember.type == String)
					{
						targetObject.name =	LangUtil.getString("messages", "clone.name.cloneof") + " " + sourceObject.label;
					}
					else if (sourceObject is InventoryItem && classMember.name == "item")
					{
						targetObject.item = cloneDomainObject(sourceObject.item);
						(targetObject.item as Item).coupledInventoryItem = targetObject as InventoryItem;
					}
					else if (	sourceObject is Rule && 
								classMember.name == "ruleItems" && 
								classMember.type == ListCollectionView)
					{
						var ris:ListCollectionView = new ArrayCollection();
						for each (var o:Object in sourceObject[classMember.name] as ListCollectionView)
						{
							var ri:RuleItem = new RuleItem();
							ri.rule = targetObject as Rule;
							ri.item = (o as RuleItem).item;
							ris.addItem(ri);
						}
						targetObject[classMember.name] = ris;
					}
					else if (sourceObject is InformationFeature && classMember.name == "matcher" && classMember.type == Matcher)
					{
						targetObject.matcher = cloneDomainObject(sourceObject.matcher);
						(targetObject.matcher as Matcher).coupledInformationFeature = targetObject as InformationFeature;
					}
					else if (	sourceObject is Matcher && 
						classMember.name == "matcherArguments" && 
						classMember.type == ListCollectionView)
					{
						var mas:ListCollectionView = new ArrayCollection();
						for each (var mao:Object in sourceObject[classMember.name] as ListCollectionView)
						{
							var ma:MatcherArgument = new MatcherArgument();
							var sourceMa:MatcherArgument = mao as MatcherArgument;
							ma.coupledMatcher = targetObject as Matcher;
							if (sourceMa.coupledArgument is DocumentDatabase ||
								sourceMa.coupledArgument is RegularExpressionGroup) // because related matchersr references these objects
								ma.coupledArgument = sourceMa.coupledArgument;
							else
								ma.coupledArgument = cloneDomainObject(sourceMa.coupledArgument);
							mas.addItem(ma);
						}
						targetObject[classMember.name] = mas;
					}
					else if (	sourceObject is InformationType && 
						classMember.name == "dataFormats" && 
						classMember.type == ListCollectionView)
					{
						targetObject[classMember.name] = sourceObject[classMember.name];
					}
					else if (classMember.type == ListCollectionView)
					{
						var list:ListCollectionView = new ArrayCollection();
						for each (var listObject:* in sourceObject[classMember.name] as ListCollectionView)
							list.addItem(cloneDomainObject(listObject));
						
						targetObject[classMember.name] = list;
					}
					else if (sourceObject[classMember.name] is AbstractEntity)
					{
						targetObject[classMember.name] = cloneDomainObject(sourceObject[classMember.name]);
					}
					else
					{
						targetObject[classMember.name] = sourceObject[classMember.name];
					}
			}
			return targetObject;
		}
	}
}