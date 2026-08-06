package com.dequeue.qr.controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/qr")
public class PublicQrController {
    @GetMapping("/v/{vendorCode}")
    public String redirect(@PathVariable String vendorCode) {
        return "Redirecting...";
    }
}
