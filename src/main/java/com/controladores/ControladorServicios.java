package com.controladores;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.modelos.Categoria;
import com.modelos.Resena;
import com.modelos.Servicio;
import com.modelos.Usuario;
import com.servicios.ServicioSubirArchivo;
import com.servicios.ServicioCategorias;
import com.servicios.ServicioCloudinary;
import com.servicios.ServicioResena;
import com.servicios.ServicioServicios;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;

import jakarta.validation.Valid;

import java.util.List;

import java.util.stream.Collectors;

@Controller
public class ControladorServicios {

    @Autowired
    private ServicioServicios servicioServicios;
    @Autowired
    private ServicioCategorias servicioCategorias;
    @Autowired
    private ServicioResena servicioResena;
    @Autowired
    private ServicioSubirArchivo fileUploadService;

    @Autowired
    private ServicioCloudinary servicioCloudinary;

    @GetMapping("/servicios/publicar")
    public String mostrarFormulario(HttpSession session, Model model) {
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioEnSesion");
        if (usuarioEnSesion == null) {
            return "redirect:/login";
        }
        cargarDatosFormulario(model, usuarioEnSesion, new Servicio(), null);
        return "nuevoServicio.jsp";
    }

    private void cargarDatosFormulario(Model model, Usuario usuario, Servicio servicio, String error) {
        List<Categoria> categorias = servicioCategorias.obtenerTodas();
        model.addAttribute("categorias", categorias);
        model.addAttribute("usuario", usuario);
        model.addAttribute("servicio", servicio);
        if (error != null) {
            model.addAttribute("error", error);
        }
    }

    @PostMapping("/publicar")
    @Transactional
    public String crearServicio(@Valid @ModelAttribute("servicio") Servicio servicio,
            BindingResult result,
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            Model model) {

        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioEnSesion");
        if (usuarioEnSesion == null)
            return "redirect:/login";

        if (result.hasErrors()) {
            System.out.println("Errores en validación del formulario:");
            result.getFieldErrors().forEach(
                    err -> System.out.println("Campo: " + err.getField() + " - Error: " + err.getDefaultMessage()));
            model.addAttribute("error", "Existen errores en los campos del formulario.");
            cargarDatosFormulario(model, usuarioEnSesion, servicio, null);
            return "nuevoServicio.jsp";
        }

        if (file.isEmpty()) {
            System.out.println("Archivo vacío recibido");
            model.addAttribute("error", "Debe subir una imagen.");
            cargarDatosFormulario(model, usuarioEnSesion, servicio, null);
            return "nuevoServicio.jsp";
        }

        //Aquí imprimimos los detalles del archivo recibido
        System.out.println("Nombre del archivo recibido: " + file.getOriginalFilename());
        System.out.println("Tamaño del archivo (bytes): " + file.getSize());

        try {
            System.out.println("Intentando subir archivo a Cloudinary...");
            String urlImagen = fileUploadService.uploadFile(file, "servicios");
            System.out.println("Imagen subida con éxito: " + urlImagen);
            servicio.setImgUrl(urlImagen);
        } catch (Exception e) {
            e.printStackTrace(); // log completo en consola
            model.addAttribute("error", "Error al subir la imagen: " + e.getMessage());
            cargarDatosFormulario(model, usuarioEnSesion, servicio, null);
            return "nuevoServicio.jsp";
        }

        servicio.setUsuario(usuarioEnSesion);
        servicioServicios.guardar(servicio);
        return "redirect:/mis-servicios";
    }

    @GetMapping("/mis-servicios")
    public String verMisServicios(HttpSession session, Model model) {
        // Verificar si el usuario está logueado
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioEnSesion");
        if (usuarioEnSesion == null) {
            return "redirect:/login"; // Redirigir a login si no está logueado
        }

        // Obtener los servicios del usuario logueado
        List<Servicio> servicios = servicioServicios.buscarPorUsuario(usuarioEnSesion);
        model.addAttribute("servicios", servicios);
        return "verMisServicios.jsp"; // Mostrar los servicios del usuario en esta vista
    }

    @GetMapping("/editar-servicio/{id}")
    public String editarServicio(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioEnSesion");
        if (usuarioEnSesion == null) {
            return "redirect:/login";
        }

        try {
            Servicio servicio = servicioServicios.obtenerPorId(id);

            if (!servicio.getUsuario().getId().equals(usuarioEnSesion.getId())) {
                return "redirect:/mis-servicios";
            }

            cargarDatosFormulario(model, usuarioEnSesion, servicio, null);
            model.addAttribute("servicio", servicio);
            return "editarServicio.jsp";
        } catch (EntityNotFoundException e) {
            return "redirect:/mis-servicios"; // Si no encuentra el servicio, redirige
        }
    }

    @PatchMapping("/actualizar-servicio/{id}")
    @Transactional
    public String actualizarServicio(@PathVariable("id") Long id,
            @Valid @ModelAttribute("servicio") Servicio servicio,
            BindingResult result,
            @RequestParam("imagen") MultipartFile imagen, // Cambiado a MultipartFile
            HttpSession session, Model model) {

        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioEnSesion");
        if (usuarioEnSesion == null) {
            return "redirect:/login";
        }

        Servicio servicioExistente = servicioServicios.obtenerPorId(id);
        if (servicioExistente == null || !servicioExistente.getUsuario().getId().equals(usuarioEnSesion.getId())) {
            return "redirect:/mis-servicios";
        }

        if (result.hasErrors()) {
            model.addAttribute("error", "Existen errores en los campos del formulario.");
            model.addAttribute("servicio", servicioExistente);
            return "editarServicio.jsp";
        }

        try {
            // Si se subió una nueva imagen
            if (!imagen.isEmpty()) {
                // Eliminar imagen anterior en Cloudinary
                servicioCloudinary.eliminarArchivo(servicioExistente.getImgUrl());

                // Subir nueva imagen a Cloudinary
                String nuevaUrl = servicioCloudinary.subirArchivo(imagen, "servicios");
                servicioExistente.setImgUrl(nuevaUrl);
            }

            // Actualizar los demás campos
            servicioExistente.setNombre(servicio.getNombre());
            servicioExistente.setDescripcion(servicio.getDescripcion());
            servicioExistente.setPrecio(servicio.getPrecio());
            servicioExistente.setCategoria(servicio.getCategoria());
            servicioExistente.setCiudad(servicio.getCiudad());

            servicioServicios.guardar(servicioExistente);
            return "redirect:/mis-servicios";

        } catch (Exception e) {
            model.addAttribute("error", "Hubo un error actualizando el servicio.");
            return "editarServicio.jsp";
        }
    }

    @PostMapping("/eliminar-servicio/{id}")
    @Transactional
    public String eliminarServicio(@PathVariable("id") Long id, Model model) {
        try {
            Servicio servicio = servicioServicios.obtenerPorId(id);
            if (servicio != null) {
                // Eliminar imagen de Cloudinary
                servicioCloudinary.eliminarArchivo(servicio.getImgUrl());

                // Eliminar servicio
                servicioServicios.eliminar(id);
            }
            return "redirect:/mis-servicios";
        } catch (Exception e) {
            model.addAttribute("error", true);
            return "redirect:/mis-servicios";
        }
    }

    @GetMapping("/servicio/detalles/{id}")
    public String verDetallesServicio(@PathVariable("id") Long id, Model model, HttpSession session) {
        Servicio servicio = servicioServicios.obtenerPorId(id);
        if (servicio == null)
            return "redirect:/servicios";

        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioEnSesion");
        boolean isAuthorInSesion = usuarioEnSesion != null
                && usuarioEnSesion.getId().equals(servicio.getUsuario().getId());

        // Obtener reseñas y promedio
        List<Resena> resenas = servicioResena.obtenerPorServicio(servicio);
        Double promedio = servicioResena.obtenerPromedioCalificacion(servicio);

        model.addAttribute("servicio", servicio);
        model.addAttribute("usuarioSesion", usuarioEnSesion);
        model.addAttribute("isAuthorInSesion", isAuthorInSesion);
        model.addAttribute("resenas", resenas);
        model.addAttribute("promedio", promedio);

        return "verServicioCompleto.jsp";
    }

    @GetMapping("/servicios")
    public String mostrarServicios(@RequestParam(value = "categoriaId", required = false) Long categoriaId, Model model,
            HttpSession session) {
        // Obtener usuario en sesión
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioEnSesion");

        List<Categoria> categoriasConServicios;

        if (categoriaId != null) {
            categoriasConServicios = servicioServicios.obtenerCategoriasConServicios()
                    .stream()
                    .filter(c -> c.getId().equals(categoriaId))
                    .collect(Collectors.toList());
        } else {
            categoriasConServicios = servicioServicios.obtenerCategoriasConServicios();
        }

        model.addAttribute("categorias", categoriasConServicios);
        model.addAttribute("usuarioSesion", usuarioEnSesion);
        return "servicios.jsp";
    }

    @GetMapping("/buscar-servicios")
    public String buscarServicios(@RequestParam("query") String query, Model model) {
        List<Servicio> servicios = servicioServicios.buscarPorNombre(query);
        System.out.println("Servicios encontrados: " + servicios.size());
        model.addAttribute("servicios", servicios);
        model.addAttribute("query", query);
        return "resultadoBusqueda.jsp";
    }

}
