package com.formacionbdi.springboot.app.item.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.formacionbdi.springboot.app.item.models.Item;
import com.formacionbdi.springboot.app.item.models.Producto;
import com.formacionbdi.springboot.app.item.models.service.ItemService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

@RefreshScope
@RestController
public class ItemController {

	private final Logger logger = LoggerFactory.getLogger(ItemController.class);
	
	@Autowired
	private Environment env;
	
	@Autowired
	private CircuitBreakerFactory cbFactory;
	
	@Autowired
	@Qualifier("serviceFeign")
	private ItemService itemService;
	
	@Value("${configuration.texto}")
	private String texto;
	
	@GetMapping("/listar")
	public List<Item> listar(@RequestParam(name="nombre", required = false) String nombre, @RequestHeader(name="token-request", required = false) String token){
		System.out.println(nombre);
		System.out.println(token);
		return itemService.findAll();
	}
	
	@GetMapping("/ver/{id}/cantidad/{cantidad}")
	public Item detalle(@PathVariable Long id, @PathVariable Integer cantidad){
		return cbFactory.create("items_guillermo")
				.run(()-> itemService.findById(id, cantidad), e -> metodoAlternativo(id, cantidad, e));
	}
	
	@CircuitBreaker(name = "items_guillermo", fallbackMethod = "metodoAlternativo")
	@GetMapping("/ver2/{id}/cantidad/{cantidad}")
	public Item detalle2(@PathVariable Long id, @PathVariable Integer cantidad){
		return itemService.findById(id, cantidad);
	}
	
	@CircuitBreaker(name = "items_guillermo", fallbackMethod = "metodoAlternativo2")
	@TimeLimiter(name = "items_guillermo")//Con esta anotacion solo se controla el timeout, no se hace los corto circuitos, ni tiempo de espera
	@GetMapping("/ver3/{id}/cantidad/{cantidad}")
	public CompletableFuture<Item> detalle3(@PathVariable Long id, @PathVariable Integer cantidad){
		return CompletableFuture.supplyAsync(() -> itemService.findById(id, cantidad));
	}
	
	public Item metodoAlternativo(Long id, Integer cantidad, Throwable e){
		logger.info(e.getMessage());
		
		Item item = new Item();
		Producto producto = new Producto();
		
		item.setCantidad(cantidad);
		producto.setId(id);
		producto.setNombre("Camara Sony");
		producto.setPrecio(500.00);
		item.setProducto(producto);
		
		return item;
	}
	
	public CompletableFuture<Item> metodoAlternativo2(Long id, Integer cantidad, Throwable e){
		logger.info(e.getMessage());
		
		Item item = new Item();
		Producto producto = new Producto();
		
		item.setCantidad(cantidad);
		producto.setId(id);
		producto.setNombre("Camara Sony");
		producto.setPrecio(500.00);
		item.setProducto(producto);
		
		return CompletableFuture.supplyAsync(() -> item);
	}
	
	@GetMapping("/obtener-config")
	public ResponseEntity<?> obtenerConfig(@Value("${server.port}") String puerto){
		
		logger.info(texto);
		
		Map<String, String> json = new HashMap<>();
		
		json.put("texto", texto);
		json.put("puerto", puerto);
		
		if (env.getActiveProfiles().length > 0  && env.getActiveProfiles()[0].equals("dev")) {

			json.put("autor.name", env.getProperty("configuration.autor.name"));
			json.put("autor.email", env.getProperty("configuration.autor.email"));
		}
		
		return new ResponseEntity<Map<String, String>>(json, HttpStatus.OK);
	}
	
}
