package io.sufeng.context.domain.entity.message;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * @Author zhangchao
 * @Date 2019/5/24 15:22
 * @Version v1.0
 */
@Getter
@Setter
@Entity
public class MessageUser {

    public enum Status {
        ONLINE, OFFLINE
    }

    @Id
    private String id;

    @Column
    private String encodedPrincipal;

    @Column
    private String nickName;

    @Column
    private String token;

    @Column
    private String channelId;

    @Column
    private String host;

    @Column
    private Status status;

}
