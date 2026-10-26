
package com.pe.universal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Byte idRol;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 30)
    private String nombreRol;
}
