package org.flyants.authorize.domain.service;

import org.flyants.authorize.configuration.PageResult;
import org.flyants.authorize.domain.entity.platform.People;
import org.flyants.authorize.dto.app.PeopleInfoDto;
import org.flyants.authorize.dto.app.SettingPasswordDto;

import java.util.Optional;

/**
 * @Author zhangchao
 * @Date 2019/4/26 14:08
 * @Version v1.0
 */
public interface PeopleService {
    Optional<People> findPeopleById(String peopleId);

    Optional<String> findByPhone(String phone);

    Optional<String> loginByPassword(String phone,String password);

    Optional<People> findByPassword(String phone,String password);

    PageResult<People> findList(Integer page, Integer size, String searchBy, String keyWord);

    void createPeople(String phone,String nickName);

    void logout(String peopleId);

    PeopleInfoDto info(String peopleId);

    void editPeopleIntroduction(String peopleId, String introduction);

    void assistPeople(String peopleId, String assistPeopleId);

    void updatePeopleInfo(String peopleId, PeopleInfoDto peopleInfoDto);

    void setPassword(String peopleId, SettingPasswordDto settingPassword);
}