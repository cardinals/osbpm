package cn.linkey.rulelib.S003;

import java.util.HashMap;

import cn.linkey.dao.Rdb;
import cn.linkey.doc.Document;
import cn.linkey.doc.Documents;
import cn.linkey.factory.BeanCtx;
import cn.linkey.form.HtmlParser;
import cn.linkey.form.ModForm;
import cn.linkey.rule.LinkeyRule;
import cn.linkey.util.Tools;
import cn.linkey.wf.ProcessEngine;

/**
 * 本规则主要负责解析流程的子表单
 * 
 * 根据所有环节中配置的子表单列表读取formbody中的内容解析后输出
 * 
 * @author Administrator
 *
 */
public class R_S003_B060 implements LinkeyRule {

	String uiType = "3"; //20180205 暂时写死easyui类型
	private static int IsBackFlag = 0; //0表示不重审，1表示重审

	@Override
	public String run(HashMap<String, Object> params) throws Exception {
		ProcessEngine linkeywf = BeanCtx.getLinkeywf();
		StringBuilder formBody = (StringBuilder) params.get("FormBody"); /* 传入的表单HTML代码 */
		StringBuilder subFormBody = new StringBuilder();/* 子表单的HTML代码 */
		HtmlParser insHtmlParser = (HtmlParser) BeanCtx.getBean("HtmlParser");/* 分析html代码并填上字段内容 */

		/* 1.获得当前环节的所有子表单的HTML代码 */
		//20180913 添加NODEID、WF_PROCESSID
		String sql = "select FormTitle,WF_OrUnid,NodeName,UserName,WF_DocCreated,NODEID,WF_PROCESSID from BPM_SubFormData where DocUnid='"
				+ linkeywf.getDocUnid() + "' and ReadOnly='1' order by WF_DocCreated";
		//String sql = "select FormTitle,WF_OrUnid,NodeName,UserName,WF_DocCreated from BPM_SubFormData where DocUnid='" + linkeywf.getDocUnid() + "' and ReadOnly='1' order by WF_DocCreated";
		Document[] dc = Rdb.getAllDocumentsBySql(sql);
		for (Document doc : dc) { 
			//20180913 添加NODEID
			//subFormBody.append("<span id=\"" + doc.g("WF_OrUnid") + "\" class=\"CollapseSubForm\" onclick=\"ExpandSubFormBody(this)\" >");
			Document currentInsUserDoc = linkeywf.getCurrentInsUserDoc();
			if(currentInsUserDoc != null){
				String sql2 = "select RemoveDuplicationSubForm from BPM_MODTASKLIST where PROCESSID='"
						+ doc.g("WF_PROCESSID") + "' and NODEID='" + currentInsUserDoc.g("NODEID") + "'";
				if ("1".equals(Rdb.getValueBySql(sql2)) && currentInsUserDoc.g("NODEID").equals(doc.g("NODEID"))) {
					IsBackFlag = 1;
					subFormBody = getCurrentSubForm(linkeywf, subFormBody, insHtmlParser, doc);
					if(Tools.isNotBlank(subFormBody.toString())){
						continue;
					}
				}
			}
			subFormBody.append("<span id=\"" + doc.g("WF_OrUnid") + "\"  Nodeid=\"" + doc.g("NODEID")
					+ "\" class=\"CollapseSubForm\" onclick=\"ExpandSubFormBody(this)\" >");
			subFormBody.append(doc.g("FormTitle")).append("(").append(doc.g("NodeName")).append(" ")
					.append(doc.g("UserName")).append(" ").append(doc.g("WF_DocCreated")).append(")");
			subFormBody.append(
					"</span><br>\n<div id=\"SUBFORM_" + doc.g("WF_OrUnid") + "\" class=\"CollapseSubFormBody\"></div>");
		}
		//		BeanCtx.out(subFormBody.toString());
		
		/* 编辑状态下只有当前环节的子表单为编辑状态 */
		if(IsBackFlag == 0){
			subFormBody = getCurrentSubForm(linkeywf, subFormBody, insHtmlParser, null);
		}

		//BeanCtx.p("subFormBody="+subFormBody); 
		/* 2.追加子表单内容到主表单中去 */
		int spos = formBody.indexOf("[SUBFORM]");
		if (spos != -1) {
			if (subFormBody.length() > 0) {
				formBody = formBody.replace(spos, spos + 9, subFormBody.toString()); //把主表单插入到子表单的指定位置
			} else {
				formBody = formBody.replace(spos, spos + 9, "<LISTSUBFORM></LISTSUBFORM>"); //换成空值
			}
		} else { /* 追加到主表单的最后 */
			formBody.append("\n<!-- SUBFORM Begin-->\n<LISTSUBFORM>\n");
			formBody.append(subFormBody);
			formBody.append("\n</LISTSUBFORM>\n<!-- SUBFORM End-->\n");
		}
		return "";
	}

	/**
	 * 
	 * @Description: 获取编辑状态下只有当前环节的子表单html
	 *
	 * @param linkeywf ProcessEngine对象
	 * @param subFormBody 子表单html
	 * @param insHtmlParser HTML解析器
	 * @return subFormBody
	 */
	private StringBuilder getCurrentSubForm(ProcessEngine linkeywf, StringBuilder subFormBody, HtmlParser insHtmlParser, Document doc)
			throws Exception {
		if (!linkeywf.isReadOnly()) {
			/* 编辑状态下只有当前环节的子表单为编辑状态 */
			String subFormNumber = linkeywf.getCurrentModNodeDoc().g("SubFormNumberLoad");
			if (Tools.isNotBlank(subFormNumber)) {
				Document subFormDoc = ((ModForm) BeanCtx.getBean("ModForm")).getFormDoc(subFormNumber);
				if (!subFormDoc.isNull()) {
					String subFormHtml = subFormDoc.g("FormBody");
					subFormHtml = insHtmlParser.parserHtml(linkeywf.getDocument(), subFormHtml, false, false, "",
							uiType);//解析子表单填上字段内容

					//BeanCtx.p("SubFormCollapsed="+linkeywf.getCurrentModNodeDoc().g("SubFormCollapsed"));
					if (linkeywf.getCurrentModNodeDoc().g("SubFormCollapsed").equals("1")) { /* 看是否需要折叠子表单 */
						String id = "CollapseSubForm_1";
						String title = linkeywf.getCurrentModNodeDoc()
								.g("SubFormCollapsedTitle"); /* 折叠子表单时可以在环节中指定标题 */
						if (Tools.isBlank(title)) {
							title = subFormDoc.g("FormName");
						}
						subFormBody.append("<span id=\"" + id + "\" Nodeid=\"" + linkeywf.getCurrentNodeid()
								+ "\" class=\"ExpandSubForm\" onclick=\"ExpandSubForm(this)\" >");
						if(doc == null){
							subFormBody.append(title);
						}else{
							subFormBody.append(doc.g("FormTitle")).append("(").append(doc.g("NodeName")).append(" ")
							.append(doc.g("UserName")).append(" ").append(doc.g("WF_DocCreated")).append(")");
						}
						subFormBody
								.append("</span><br>\n<div id=\"SUBFORM_" + id + "\" class=\"CollapseSubFormBody\">\n");
						subFormBody.append(subFormHtml); /* 解析后追加 */
						subFormBody.append("\n</div>");
					} else {
						/* 不需要折叠 */
						subFormBody.append(subFormHtml);
						/* 解析后追加 */
					}
				}
			}
		}
		return subFormBody;
	}
}
