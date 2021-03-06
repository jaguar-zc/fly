package io.sufeng.web.v1.oauth;

import io.sufeng.context.domain.service.AuthorizeService;
import io.sufeng.context.domain.service.PeopleService;
import lombok.extern.slf4j.Slf4j;
import io.sufeng.context.domain.entity.oauth2.OAuthAuthorizeRequest;
import io.sufeng.context.domain.entity.oauth2.OAuthClient;
import io.sufeng.context.domain.entity.People;
import io.sufeng.context.utils.ResourceUtils;
import io.sufeng.context.utils.ResponseDataUtils;
import io.sufeng.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

/**
 *
 * 授权码模式
 * @Author zhangchao
 * @Date 2019/4/25 11:48
 * @Version v1.0
 */
@Slf4j
@Controller
@RequestMapping("/api/v1.0/oauth2")
public class AuthorizeCodeController {


    @Autowired
    private AuthorizeService authorizeService;


    @Autowired
    private PeopleService peopleService;

    /**
     * 授权码模式（authorization code） 跳转到用户授权的界面
     * @param response_type
     * @param client_id
     * @param redirect_uri
     * @param scope
     * @param state
     * @return
     */
    @GetMapping(value = "/authorize",params = {"response_type=code"})
    public Object authorizeCodeStart(String response_type, String client_id, String redirect_uri, String scope, String state)  {
        if (!authorizeService.checkClientId(client_id)) {
            return ResponseDataUtils.buildError("无效的 client_id");
        }
        ModelAndView mav = new ModelAndView();
        mav.addObject("response_type",response_type);
        mav.addObject("client_id",client_id);
        mav.addObject("redirect_uri",redirect_uri);
        mav.addObject("scope",scope);
        mav.addObject("state",state);

        OAuthClient oAuthClient = authorizeService.findOAuthClinetByClientId(client_id);
        String clientIcon = oAuthClient.getClientIcon();
        String clientName = oAuthClient.getClientName();
        String clientServerDomain = oAuthClient.getClientServerDomain();
        String resource = oAuthClient.getOAuthClientResource().getResource();
        mav.addObject("resource",resource);
        mav.addObject("clientIcon",clientIcon);
        mav.addObject("clientName",clientName);
        mav.addObject("clientServerDomain",clientServerDomain);

        log.info("check login:{}"+ResourceUtils.isLogin());
        if(ResourceUtils.isLogin()){
            People people = ResourceUtils.getCurrentPeople();
            mav.addObject("encodedPrincipal",people.getEncodedPrincipal());
            mav.addObject("login","1");
            mav.setViewName("oauth/authorize");
        }else{
            mav.addObject("login","0");
            mav.setViewName("oauth/authorize_login");
        }
        return mav;
    }


    /**
     * 授权码模式（authorization code） 用户点击界面的 同意/拒绝 按钮
     * @param response_type
     * @param client_id
     * @param redirect_uri
     * @param scope
     * @param state
     * @return
     */
    @PostMapping(value = "/authorize",params = {"response_type=code"})
    public Object authorizeCodeComform(String response_type, String client_id, String redirect_uri, String scope, String state,String login,String username,String password)  {
        if (!authorizeService.checkClientId(client_id)) {
            return ResponseDataUtils.buildError("无效的 client_id");
        }

        if("0".equals(login)){//未登录
            Optional<People> optional = peopleService.findPeopleByPassword(username, password);
            People people = optional.orElseThrow(() -> new BusinessException("用户名密码错误"));
            ResourceUtils.setLoginPeople(people);
        }

        if("1".equals(login)){//已登录

        }

        //TODO 点击同意 已登录判断是否同意
        //todo 未登录 判断输入的用户名密码是否正确

        OAuthAuthorizeRequest authorize = authorizeService.authorization(response_type,client_id,redirect_uri,scope,state);
        String authCode = authorize.getAuthorizationCode();
        System.out.println("授权码="+authCode);
        //获取客户端重定向地址
        ModelAndView mav = new ModelAndView();
        mav.setViewName("redirect:"+redirect_uri+"?code="+authCode+"&state="+state);
        return mav;
    }



}
