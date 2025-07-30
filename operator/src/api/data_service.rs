use actix_web::{get, http::header::ContentType, web, HttpResponse, Responder};

#[get("/items")]
async fn items() -> impl Responder {
    HttpResponse::Ok()
        .content_type(ContentType::json())
        .body(include_str!("../../assets/minecraft-data/items.json"))
}

pub fn configure(app: &mut web::ServiceConfig) {
    app.service(
        web::scope("/data")
            .service(items),
    );
}
