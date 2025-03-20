package com.servicios;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.modelos.Solicitud;
import com.modelos.Usuario;
import com.repositorios.SolicitudRepositorio;

@Service
public class SolicitudServicio {

    @Autowired
    private SolicitudRepositorio solicitudRepositorio;

    // Guardar solicitud
    public void guardarSolicitud(Solicitud solicitud) {
        solicitudRepositorio.save(solicitud);
    }

    // Obtener solicitudes enviadas por un usuario
    public List<Solicitud> obtenerSolicitudesPorSolicitante(Usuario solicitante) {
        return solicitudRepositorio.findBySolicitante(solicitante);
    }

    public List<Solicitud> obtenerTodasLasSolicitudes() {
        return solicitudRepositorio.findAll();
    }
     // Obtener solicitudes de los servicios del usuario (proveedor)
     public List<Solicitud> obtenerSolicitudesPorProveedor(Usuario proveedor) {
        // Filtrar las solicitudes cuya relación con el servicio sea de un proveedor específico
        return solicitudRepositorio.findByServicio_Usuario(proveedor);
    }

}
