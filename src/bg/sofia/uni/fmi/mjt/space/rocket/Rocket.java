package bg.sofia.uni.fmi.mjt.space.rocket;

import java.util.Objects;
import java.util.Optional;

public record Rocket(String id, String name, Optional<String> wiki, Optional<Double> height) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Rocket rocket = (Rocket) o;
        return Objects.equals(id, rocket.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}