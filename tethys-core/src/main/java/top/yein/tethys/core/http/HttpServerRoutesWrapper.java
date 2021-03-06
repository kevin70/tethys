package top.yein.tethys.core.http;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * {@link HttpServerRoutes} 包装器.
 *
 * @author KK (kzou227@qq.com)
 */
@Log4j2
public class HttpServerRoutesWrapper implements HttpServerRoutes {

  private final HttpServerRoutes routes;
  private final HttpExceptionHandler httpExceptionHandler;

  /**
   * 构造函数.
   *
   * @param routes 被包装的路由对象
   */
  public HttpServerRoutesWrapper(HttpServerRoutes routes) {
    this(routes, new HttpExceptionHandler());
  }

  /**
   * @param routes
   * @param httpExceptionHandler
   */
  public HttpServerRoutesWrapper(
      HttpServerRoutes routes, HttpExceptionHandler httpExceptionHandler) {
    this.routes = routes;
    this.httpExceptionHandler = httpExceptionHandler;
  }

  @Override
  public HttpServerRoutes directory(
      String uri,
      Path directory,
      @Nullable Function<HttpServerResponse, HttpServerResponse> interceptor) {
    return routes.directory(uri, directory, interceptor);
  }

  @Override
  public HttpServerRoutes route(
      Predicate<? super HttpServerRequest> condition,
      BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>>
          handler) {
    return routes.route(condition, handler);
  }

  @Override
  public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
    return Flux.defer(() -> routes.apply(request, response))
        .onErrorResume(t -> httpExceptionHandler.apply(request, response, t));
  }
}
