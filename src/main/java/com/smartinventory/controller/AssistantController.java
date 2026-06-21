package com.smartinventory.controller;

import com.smartinventory.service.AssistantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * AI Assistant chatbot — serves the chat page and answers questions over a
 * simple GET JSON endpoint (GET avoids CSRF since it is read-only).
 */
@Controller
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /** The chat page. */
    @GetMapping("/ai/assistant")
    public String page(Model model) {
        model.addAttribute("active", "ai-assistant");
        return "ai-assistant";
    }

    /** JSON answer endpoint used by the chat UI. */
    @GetMapping("/api/assistant")
    @ResponseBody
    public Map<String, Object> ask(@RequestParam(value = "q", defaultValue = "") String q) {
        return assistantService.ask(q);
    }
}
