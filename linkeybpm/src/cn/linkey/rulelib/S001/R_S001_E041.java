package cn.linkey.rulelib.S001;

import java.util.*;
import cn.linkey.app.AppUtil;
import cn.linkey.dao.Rdb;
import cn.linkey.doc.Document;
import cn.linkey.factory.BeanCtx;
import cn.linkey.rule.LinkeyRule;
import cn.linkey.util.Tools;

/**
 * @RuleName:新增流程子表单事件
 * @author admin
 * @version:
 * @Created: 2014-03-30 23:09
 */
public class R_S001_E041 implements LinkeyRule {

    @Override
    public String run(HashMap<String, Object> params) {
        //获取事件运行参数
        Document formDoc = (Document) params.get("FormDoc"); //表单配置文档
        Document doc = (Document) params.get("DataDoc"); //数据主文档
        String eventName = (String) params.get("EventName");//事件名称
        if (eventName.equals("onFormOpen")) {
            String readOnly = (String) params.get("ReadOnly"); //1表示只读，0表示编辑
            return onFormOpen(doc, formDoc, readOnly);
        }
        else if (eventName.equals("onFormSave")) {
            return onFormSave(doc, formDoc);
        }
        return "1";
    }

    public String onFormOpen(Document doc, Document formDoc, String readOnly) {
        //当表单打开时
        if (readOnly.equals("1")) {
            return "1";
        } //如果是阅读状态则可不执行
        if (doc.isNewDoc()) {
            //可以对表单字段进行初始化如:doc.s("fdname",fdvalue),可以获取字段值 doc.g("fdname")

            String appid = BeanCtx.g("WF_Appid", true);
            String sql = "select FormNumber from BPM_FormList where WF_Appid='" + appid + "' and FormType='3' order by FormNumber desc";
            String newNum = AppUtil.getElNewNum(sql);
            if (Tools.isBlank(newNum)) {
                newNum = "F_" + appid + "_S001";
            }
            doc.s("FormNumber", newNum);
        }
        
		// 20180420 设置默认UI类型
		String sql1 = "select CONFIGVALUE from BPM_SYSTEMCONFIG where CONFIGID='DEFAULT_UI_ID'";
		String UIType = Rdb.getValueBySql(sql1);
		doc.s("UIType", UIType);
        
        return "1"; //成功必须返回1，否则表退出并显示返回的字符串
    }

    public String onFormSave(Document doc, Document formDoc) {
        //当表单存盘前
        doc.s("Status", "1");
        doc.s("FormType", "3");
        doc.s("Title", doc.g("FormName"));
        return "1"; //成功必须返回1，否则表示退出存盘
    }

}