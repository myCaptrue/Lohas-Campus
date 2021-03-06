package com.wx.happy.superadmin.shopadmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wx.happy.dto.ProductCategoryExecution;
import com.wx.happy.dto.Result;
import com.wx.happy.dto.ShopExcution;
import com.wx.happy.entity.*;
import com.wx.happy.enums.ProductCategoryStateEnum;
import com.wx.happy.enums.ShopStateEnum;
import com.wx.happy.service.AreaService;
import com.wx.happy.service.ProductCategoryService;
import com.wx.happy.service.ShopCategoryService;
import com.wx.happy.service.ShopService;
import com.wx.happy.util.CodeUtil;
import com.wx.happy.util.HttpServletUtil;
import com.wx.happy.util.ImageUtil;
import com.wx.happy.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/shopadmin")
public class ShopManagementController {
    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopCategoryService shopCategoryService;
    @Autowired
    private AreaService areaService;


    @RequestMapping(value = "/getshopmanagementinfo",method = RequestMethod.GET)
    @ResponseBody
    private Map<String,Object>getShopManagementInfo(HttpServletRequest request){
        Map<String,Object>modelMap=new HashMap<String, Object>();
        Long shopId=HttpServletUtil.getLong(request,"shopid");
        System.out.println(shopId);
        if(shopId<=0){
            Object currentShopObj=request.getSession().getAttribute("currentShop");
            if(currentShopObj==null){
                modelMap.put("redirect",true);
                modelMap.put("url","/ssm/shopadmin/shoplist");
            }else{
                Shop currentShop=(Shop)currentShopObj;
                modelMap.put("redirect",false);
                modelMap.put("shopId",currentShop.getShopId());
            }
        }else{
            Shop currentShop=new Shop();
            currentShop.setShopId(shopId);
            request.getSession().setAttribute("currentShop",currentShop);
            modelMap.put("redirect",false);
        }
        return modelMap;
    }




    @RequestMapping(value = "/getshoplist",method =RequestMethod.GET )
    @ResponseBody
    private Map<String,Object>getShopList(HttpServletRequest request){
        Map<String,Object>modelMap=new HashMap<String,Object>();
        PersonInfo user=new PersonInfo();
        user.setUserId(1L);
        request.getSession().setAttribute("user",user);
        user=(PersonInfo)request.getSession().getAttribute("user");
        long employeeId=user.getUserId();
        List<Shop>shopList=new ArrayList<Shop>();
        try{
            Shop shopCondition=new Shop();
            shopCondition.setOwner(user);
            ShopExcution se=shopService.getShopList(shopCondition,0,100);
            modelMap.put("shopList",se.getShopList());
            modelMap.put("user",user);
            modelMap.put("success",true);

        }catch (Exception e){
            modelMap.put("success",false);
            modelMap.put("errorMsg",e.getMessage());
        }


        return modelMap;

    }



    @RequestMapping(value = "/getshopbyid",method=RequestMethod.GET)
    @ResponseBody
    private Map<String,Object>getShopById(HttpServletRequest request){
        Map<String,Object> modelMap=new HashMap<String,Object>();
        Long shopId=HttpServletUtil.getLong(request,"shopid");
        if(shopId>-1){
            try{
                Shop shop=shopService.getShopById(shopId);
                List<Area>areaList=areaService.getAreaList();
                System.out.println(shop.getShopName());
                modelMap.put("shop",shop);
                modelMap.put("areaList",areaList);
                modelMap.put("success",true);
            }catch (Exception e){
                modelMap.put("success",false);
                modelMap.put("errMsg",e.toString());
            }


        }
        return modelMap;
    }


    @RequestMapping(value = "/getshopinitinfo",method = RequestMethod.GET)
    @ResponseBody
    private Map<String,Object>getShopinitInfo(){
       Map<String,Object>modelMap=new HashMap<String, Object>();
        List<ShopCategory>shopCategoryList=new ArrayList<ShopCategory>();
        List<Area>areaList=new ArrayList<Area>();

        try{
            shopCategoryList=shopCategoryService.getShopCategoryList(new ShopCategory());
            areaList=areaService.getAreaList();
            modelMap.put("shopCategoryList",shopCategoryList);
            modelMap.put("areaList",areaList);
            modelMap.put("success",true);
        }catch (Exception e){
            modelMap.put("success",false);
            modelMap.put("errorMsg",e.getMessage());
        }
        return modelMap;
    }



    @RequestMapping(value = "/modifyshop",method= RequestMethod.GET)
    @ResponseBody
    private Map<String,Object> modifyShop(HttpServletRequest request){
        //1.接收并转化响应的参数

        Map<String,Object>modelMap=new HashMap<String,Object>();
        String shopStr=HttpServletUtil.getString(request,"shopStr");
        ObjectMapper mapper=new ObjectMapper();
        if(!CodeUtil.checkVerifyCode(request)){
            modelMap.put("success",false);
            modelMap.put("errMsg","输入了错误的验证码");
            return modelMap;
        }


        Shop shop=null;
        try{
            shop=mapper.readValue(shopStr,Shop.class);
        }catch(Exception e){
            modelMap.put("suceess",false);
            modelMap.put("errorMsg",e.getMessage());
            return modelMap;
        }
        CommonsMultipartFile shopImg=null;
        CommonsMultipartResolver commonsMultipartResolver=new
                CommonsMultipartResolver(request.getSession().getServletContext());
        if(commonsMultipartResolver.isMultipart(request)){
            MultipartHttpServletRequest multipartHttpServletRequest=(MultipartHttpServletRequest)request;
            shopImg=(CommonsMultipartFile)multipartHttpServletRequest.getFile("shopImg");
        }
        //2.修改店铺
        if(shop!=null&&shop.getShopId()!=null){

            ShopExcution se;
            if(shopImg==null){//不修改图片
                File shopImgFile=new File(PathUtil.getImageBaesPath()+ImageUtil.getRamdomFileName());
                try {
                    shopImgFile.createNewFile();
                } catch (IOException e) {
                    modelMap.put("suceess",false);
                    modelMap.put("errorMsg",e.getMessage());
                    return modelMap;
                }
                try {
                    inputStreamToFile(shopImg.getInputStream(),shopImgFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                se= shopService.modifyShop(shop,shopImgFile);
            }
            else
                se= shopService.modifyShop(shop,null);
            if(se.getState()== ShopStateEnum.SUCCESS.getState()){
                modelMap.put("success",true);
            }
            else{
                modelMap.put("success",false);
                modelMap.put("errMsg",se.getStateInfo());
            }
            return modelMap;
        }else{
            modelMap.put("suceess",false);
            modelMap.put("errorMsg","请输入店铺id");
            return modelMap;
        }

        //3.返回结果
    }


    @RequestMapping(value = "/registershop",method= RequestMethod.GET)
    @ResponseBody
    private Map<String,Object> registerShop(HttpServletRequest request){
        //1.接收并转化响应的参数

       Map<String,Object>modelMap=new HashMap<String,Object>();
       String shopStr=HttpServletUtil.getString(request,"shopStr");
        ObjectMapper mapper=new ObjectMapper();
        if(!CodeUtil.checkVerifyCode(request)){
            modelMap.put("success",false);
            modelMap.put("errMsg","输入了错误的验证码");
            return modelMap;
        }


        Shop shop=null;
        try{
            shop=mapper.readValue(shopStr,Shop.class);
        }catch(Exception e){
            modelMap.put("suceess",false);
            modelMap.put("errorMsg",e.getMessage());
            return modelMap;
        }
        CommonsMultipartFile shopImg=null;
        CommonsMultipartResolver commonsMultipartResolver=new
                CommonsMultipartResolver(request.getSession().getServletContext());
        if(commonsMultipartResolver.isMultipart(request)){
            MultipartHttpServletRequest multipartHttpServletRequest=(MultipartHttpServletRequest)request;
            shopImg=(CommonsMultipartFile)multipartHttpServletRequest.getFile("shopImg");
        }else {
            modelMap.put("suceess",false);
            modelMap.put("errorMsg","上传图片不能为空");
        }
        //2.注册店铺
        if(shop!=null&&shopImg!=null){
            PersonInfo owner=(PersonInfo)request.getSession().getAttribute("usr");
            shop.setOwner(owner);
            File shopImgFile=new File(PathUtil.getImageBaesPath()+ImageUtil.getRamdomFileName());
            try {
                shopImgFile.createNewFile();
            } catch (IOException e) {
                modelMap.put("suceess",false);
                modelMap.put("errorMsg",e.getMessage());
                return modelMap;
            }
            try {
                inputStreamToFile(shopImg.getInputStream(),shopImgFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ShopExcution se= shopService.addShop(shop,shopImgFile);
            if(se.getState()== ShopStateEnum.CHECK.getState()){
                modelMap.put("success",true);
                List<Shop>shopList=(List<Shop>)request.getSession().getAttribute("shopList");
                if(shopList==null||shopList.size()==0){
                    shopList=new ArrayList<Shop>();
                }
                shopList.add(se.getShop());
                request.getSession().setAttribute("shopList",shopList);
            }
            else{
                modelMap.put("success",false);
                modelMap.put("errMsg",se.getStateInfo());
            }
            return modelMap;
        }else{
            modelMap.put("suceess",false);
            modelMap.put("errorMsg","请输入店铺信息");
            return modelMap;
        }




        //3.返回结果
    }


    private static void inputStreamToFile(InputStream ins, File file){
        OutputStream os=null;
        try{
            os=new FileOutputStream(file);
            int bytesRead=0;
            byte[] buffer=new byte[1024];
            while((bytesRead=ins.read(buffer))!=-1){
                os.write(buffer,0,bytesRead);
            }
        }catch(Exception e){
            throw new RuntimeException("inputStreamToFile Exception"+e.getMessage());

        }finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (ins != null) {
                    ins.close();
                }
            }catch (IOException e){
                throw new RuntimeException("close IO Exception"+e.getMessage());

            }
        }
    }








}
