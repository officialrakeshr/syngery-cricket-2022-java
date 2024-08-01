package com.aztechsynergy.crickScore.model;

import lombok.*;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "substitution")
public class Substitution {
    @NaturalId
    @Id
    private String lookUp;
    private String matchNo;
    private String username;
    @Builder.Default
    private Integer total = 0;
    @Builder.Default
    private Integer free = 0;
    @Builder.Default
    private Integer used = 0;
}
