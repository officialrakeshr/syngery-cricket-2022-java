package com.aztechsynergy.crickScore.model;

import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "substitution")
public class Substitution {
    @Id
    private String username;
    private Integer total;
    private Integer used;
}
