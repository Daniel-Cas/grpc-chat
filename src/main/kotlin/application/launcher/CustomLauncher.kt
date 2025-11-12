package com.castle.application.launcher

import io.vertx.launcher.application.VertxApplication
import io.vertx.launcher.application.VertxApplicationHooks

class CustomLauncher(args: Array<String>, hooks: VertxApplicationHooks) : VertxApplication(args, hooks)
