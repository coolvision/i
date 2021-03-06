package org.wf.dp.dniprorada.liqPay;

import static org.wf.dp.dniprorada.liqPay.LiqBuyUtil.base64_encode;
import static org.wf.dp.dniprorada.liqPay.LiqBuyUtil.sha1;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wf.dp.dniprorada.base.dao.AccessDataDao;
import org.wf.dp.dniprorada.constant.Currency;
import org.wf.dp.dniprorada.constant.Language;
import org.wf.dp.dniprorada.rest.HttpRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wf.dp.dniprorada.util.GeneralConfig;

@Component()
public class LiqBuy {

    @Autowired
    private AccessDataDao accessDataDao;

    @Autowired
    GeneralConfig generalConfig;
    
    
    private static final Logger log = LoggerFactory.getLogger(LiqBuy.class);
    private static final String sURL_Liqpay = "https://www.liqpay.com/api/checkout";
    private static final String version = "3";
    public static final Language DEFAULT_LANG = Language.RUSSIAN;
    private static final String sandbox = "1";
    private static final String payButtonHTML = new StringBuilder()
            .append("<form method=\"POST\" action=\"")
            .append(sURL_Liqpay)
            .append("\" ")
            .append("accept-charset=\"utf-8\">")
            .append("<input type=\"hidden\" name=\"data\" value=\"%s\"/>")
            .append("<input type=\"hidden\" name=\"signature\" value=\"%s\"/>")
            .append("<input type=\"image\" src=\"https://static.liqpay.com/buttons/p1%s.radius.png\"/>")
            .append("</form>").toString();
    //result = result.replaceAll("\\Q//static.liqpay.com\\E", "https://static.liqpay.com");

    public String getPayButtonHTML_LiqPay(String sID_Merchant, String sSum,
            Currency oID_Currency, Language oLanguage, String sDescription,
            String sID_Order, String sURL_CallbackStatusNew,
            String sURL_CallbackPaySuccess, Long nID_Subject, boolean bTest) throws Exception {
        
        if (oLanguage == null) {
            oLanguage = DEFAULT_LANG;
        }
        
        String sAccessKey_Merchant = accessDataDao.setAccessData(""+nID_Subject);
        
        Map<String, String> paramMerchant = new HashMap<String, String>();
        paramMerchant.put("sID", sID_Merchant);
        paramMerchant.put("nID_Subject", String.valueOf(nID_Subject));
        paramMerchant.put("sAccessKey", sAccessKey_Merchant);
        
        //String soJSON_Merchant = HttpRequester.get("https://test.igov.org.ua/wf-central/service/merchant/getMerchant", paramMerchant);
        String soJSON_Merchant = HttpRequester.get(generalConfig.sHostCentral() + "/wf-central/service/merchant/getMerchant", paramMerchant);
        log.info("soJSON_Merchant="+soJSON_Merchant);
        
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(soJSON_Merchant);
        
        String publicKey = sID_Merchant;
        String privateKey = (String) jsonObject.get("sPrivateKey");
        if (privateKey == null) {
            privateKey = "test";
        }
        if (sURL_CallbackStatusNew == null) {
            if(jsonObject.get("sURL_CallbackStatusNew")!=null){
                sURL_CallbackStatusNew = (String) jsonObject.get("sURL_CallbackStatusNew");
            }else{
                sURL_CallbackStatusNew = "";
            }
        }
        log.info("sURL_CallbackStatusNew="+sURL_CallbackStatusNew);
        
        
        if (sURL_CallbackPaySuccess == null) {
            if(jsonObject.get("sURL_CallbackPaySuccess")!=null){
                sURL_CallbackPaySuccess = (String) jsonObject.get("sURL_CallbackPaySuccess");
            }else{
                sURL_CallbackPaySuccess = "https://igov.org.ua";
            }
        }
        log.info("sURL_CallbackPaySuccess="+sURL_CallbackPaySuccess);
        
        
        if (sURL_CallbackStatusNew != null) {
            log.info("nID_Subject="+nID_Subject);
            if(nID_Subject==null){
                nID_Subject=new Long(0);
            }
            String snID_Subject=""+nID_Subject;
            log.info("snID_Subject="+snID_Subject);
            
            log.info("accessDataDao!=null:"+(accessDataDao!=null));
            //String nID_Access = accessDataDao.setAccessData(String.valueOf(nID_Subject));
            String sAccessKey = accessDataDao.setAccessData(snID_Subject);
            
            sURL_CallbackStatusNew = new StringBuilder(sURL_CallbackStatusNew)
                    .append(sURL_CallbackStatusNew.indexOf("?")>-1?"&":"?")
                    .append("nID_Subject=").append(nID_Subject)
                    .append("&sAccessKey=").append(sAccessKey).toString();
        }
        log.info("sURL_CallbackStatusNew(with security-key)="+sURL_CallbackStatusNew);
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("version", version);
        params.put("amount", sSum);
        params.put("currency", oID_Currency.name());
        params.put("language", oLanguage.getShortName());
        params.put("description", sDescription);
        params.put("order_id", sID_Order);
        params.put("server_url", sURL_CallbackStatusNew);
        params.put("result_url", sURL_CallbackPaySuccess);
        params.put("public_key", publicKey);

        if (bTest) {
            params.put("sandbox", sandbox);
        }
        
        log.info("getPayButtonHTML_LiqPay params: " + params + " privateKey: " + privateKey);
        String result = getForm(params, privateKey, oLanguage);
        log.info("getPayButtonHTML_LiqPay ok!: " + result);
        //result = result.replaceAll("\\Q//static.liqpay.com\\E", "https://static.liqpay.com");
        //log.info("getPayButtonHTML_LiqPay ok-replaced!: " + result);
        //static.liqpay.com/buttons/p1ru.radius.png
        
        // LiqPay liqpay = new LiqPay(publicKey, privateKey);
        // String result = liqpay.cnb_form(params);
        // System.out.println(result);
        return result;
    }

    private String getForm(Map<String, String> params, String privateKey, Language oLanguage) {
        String data = base64_encode(JSONObject.toJSONString(params));
        String signature = createSignature(data, privateKey);
        return String.format(payButtonHTML, data, signature, oLanguage.getShortName());
    }

    private String str_to_sign(String str) {
        return base64_encode(sha1(str));
    }

    private String createSignature(String base64EncodedData, String privateKey) {
        return str_to_sign(privateKey + base64EncodedData + privateKey);
    }
}
